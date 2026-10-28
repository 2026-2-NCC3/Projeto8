package br.org.proximaetapa.app.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.Repositorio;

/**
 * RFM06 — Registro de presença por QR Code.
 *
 * O aluno aponta a câmera para o QR projetado no encontro. A presença é
 * gravada no celular IMEDIATAMENTE e só depois o app tenta enviá-la. Esse
 * detalhe é o que diferencia a Entrega 2: nos espaços onde a ONG atua o sinal
 * costuma falhar, e uma presença perdida significa um certificado a menos.
 */
public class CheckinActivity extends AppCompatActivity {

    private TextView txtResultado;
    private TextView txtStatusConexao;
    private TextView txtPendentes;
    private Button btnEscanear;
    private Repositorio repositorio;

    /** Leitor de QR Code da biblioteca ZXing. */
    private final ActivityResultLauncher<ScanOptions> leitorQr =
            registerForActivityResult(new ScanContract(), this::tratarLeitura);

    /** Permissão de câmera, pedida só quando o aluno decide escanear. */
    private final ActivityResultLauncher<String> pedirCamera =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedida -> {
                if (concedida) {
                    abrirLeitor();
                } else {
                    mostrarResultado(getString(R.string.camera_negada), false);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin);

        repositorio = Repositorio.obter(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        txtResultado = findViewById(R.id.txtResultado);
        txtStatusConexao = findViewById(R.id.txtStatusConexao);
        txtPendentes = findViewById(R.id.txtPendentes);
        btnEscanear = findViewById(R.id.btnEscanear);

        btnEscanear.setOnClickListener(v -> verificarPermissaoEEscanear());
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarStatus();
    }

    private void verificarPermissaoEEscanear() {
        boolean temPermissao = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        if (temPermissao) {
            abrirLeitor();
        } else {
            pedirCamera.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirLeitor() {
        ScanOptions opcoes = new ScanOptions();
        opcoes.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        opcoes.setPrompt(getString(R.string.aponte_para_qr));
        opcoes.setBeepEnabled(true);
        opcoes.setOrientationLocked(false);
        opcoes.setCaptureActivity(CapturaQrActivity.class);
        leitorQr.launch(opcoes);
    }

    private void tratarLeitura(ScanIntentResult resultado) {
        if (resultado.getContents() == null) {
            // O aluno voltou sem escanear — não é erro, não precisa de alarde.
            return;
        }
        registrarPresenca(resultado.getContents());
    }

    private void registrarPresenca(String qrCode) {
        btnEscanear.setEnabled(false);
        mostrarResultado(getString(R.string.registrando_presenca), true);

        repositorio.registrarCheckin(qrCode, new Repositorio.Callback<String>() {
            @Override
            public void sucesso(String mensagem) {
                btnEscanear.setEnabled(true);
                mostrarResultado(mensagem, true);
                atualizarStatus();
            }

            @Override
            public void falha(String mensagem) {
                btnEscanear.setEnabled(true);
                mostrarResultado(mensagem, false);
                atualizarStatus();
            }
        });
    }

    private void mostrarResultado(String mensagem, boolean positivo) {
        txtResultado.setVisibility(View.VISIBLE);
        txtResultado.setText(mensagem);
        txtResultado.setTextColor(ContextCompat.getColor(this,
                positivo ? R.color.primary : R.color.erro));
    }

    /** Mostra se está online e quantos check-ins ainda esperam para subir. */
    private void atualizarStatus() {
        boolean online = repositorio.temConexao();
        txtStatusConexao.setText(online ? R.string.status_online : R.string.status_offline);
        txtStatusConexao.setTextColor(ContextCompat.getColor(this,
                online ? R.color.primary : R.color.aviso));

        repositorio.contarPendentes(new Repositorio.Callback<Integer>() {
            @Override
            public void sucesso(Integer pendentes) {
                if (pendentes > 0) {
                    txtPendentes.setVisibility(View.VISIBLE);
                    txtPendentes.setText(getResources().getQuantityString(
                            R.plurals.presencas_pendentes, pendentes, pendentes));
                } else {
                    txtPendentes.setVisibility(View.GONE);
                }
            }

            @Override
            public void falha(String mensagem) {
                txtPendentes.setVisibility(View.GONE);
            }
        });
    }
}
