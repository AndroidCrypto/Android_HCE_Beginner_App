package de.androidcrypto.android_hce_beginner_app;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.util.Log;

// Você precisará de uma classe Utils.java para a conversão de Hex, se ela não existir
// Por enquanto, vamos assumir que as conversões básicas estão em um arquivo auxiliar.

public class MyHostApduService extends HostApduService {

    // Função de conversão de Hex para Byte Array (Se o projeto original não tiver)
    // Se o Android Studio reclamar de Utils, crie um arquivo chamado Utils.java e coloque essas funções lá.
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        if (commandApdu == null) {
            return hexStringToByteArray("6F00"); // Erro genérico
        }

        // Simulação de SELECT AID (APDU inicial)
        // Se a maquininha mandar um SELECT AID (00A40400...), respondemos com a track gravada
        String hexCommand = bytesToHex(commandApdu);
        Log.d("HCE_LOG", "Recebido da Maquininha: " + hexCommand);

        // O comando de seleção geralmente começa com 00A40400
        if (hexCommand.startsWith("00A40400")) {
            // Aqui pegamos a TRACK que você gravou na tela principal
            SharedPreferences prefs = getSharedPreferences("NfcData", MODE_PRIVATE);
            // Valor padrão: 9000 (Status OK). Se não tiver track gravada, o app continua funcionando.
            String trackData = prefs.getString("TRACK_DATA", "9000"); 
            
            Log.d("HCE_LOG", "Respondendo com Track gravada: " + trackData);
            return hexStringToByteArray(trackData);
        }

        // Resposta padrão para outros comandos (Status OK)
        return hexStringToByteArray("9000");
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d("HCE_LOG", "Conexão terminada. Razão: " + reason);
    }

    // Função de conversão de Byte Array para Hex String (Para Log)
    private static String bytesToHex(byte[] bytes) {
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
