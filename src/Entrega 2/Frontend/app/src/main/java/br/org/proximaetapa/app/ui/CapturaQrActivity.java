package br.org.proximaetapa.app.ui;

import com.journeyapps.barcodescanner.CaptureActivity;

/**
 * Tela de câmera do leitor de QR Code.
 *
 * A biblioteca ZXing já traz uma CaptureActivity pronta, mas estendê-la e
 * declarar esta no nosso AndroidManifest permite controlar a orientação da
 * tela e o comportamento do teclado — sem isso, girar o celular durante a
 * leitura reinicia a câmera.
 */
public class CapturaQrActivity extends CaptureActivity {
}
