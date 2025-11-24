package de.androidcrypto.android_hce_beginner_app;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    EditText etTrackData;
    Button btnGravar;
    TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Busca os elementos do layout (que vamos criar no próximo passo)
        etTrackData = findViewById(R.id.etTrackData); 
        btnGravar = findViewById(R.id.btnGravar);     
        tvStatus = findViewById(R.id.tvStatus);       

        // 1. Recuperar track salva anteriormente
        SharedPreferences prefs = getSharedPreferences("NfcData", MODE_PRIVATE);
        // Valor padrão de exemplo
        String savedTrack = prefs.getString("TRACK_DATA", "00A4040007F00102030405069000"); 
        etTrackData.setText(savedTrack);

        // 2. Configura o botão de Gravar
        btnGravar.setOnClickListener(view -> {
            String newData = etTrackData.getText().toString().trim();
            if (newData.isEmpty()) {
                Toast.makeText(this, "A track não pode estar vazia!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Salva na memória permanente (SharedPreferences)
            SharedPreferences.Editor editor = getSharedPreferences("NfcData", MODE_PRIVATE).edit();
            editor.putString("TRACK_DATA", newData);
            editor.apply();

            tvStatus.setText("Track Gravada com Sucesso!\nPronto para aproximar da maquininha.");
            Toast.makeText(this, "Dados NFC atualizados!", Toast.LENGTH_SHORT).show();
        });
        
        // Exibe o status inicial
        tvStatus.setText("Última Track carregada. Clique em Gravar para confirmar.");
    }
}
