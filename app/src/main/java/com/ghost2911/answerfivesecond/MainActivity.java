package com.ghost2911.answerfivesecond;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    TextView tvTimer, tvQuestion;
    ArrayList<String> questions = new ArrayList<>();
    ArrayList<String> audios = new ArrayList<>();
    byte activePlayer = 1;
    byte maxPlayers = 2;
    boolean secondFail = false;
    ImageView btnRunTimer;
    Button btnAudio;
    ImageView btnSettings;
    ImageView btnRestart;
    CountDownTimer Count;
    boolean isRun = false;
    PlayerInfo[] pInfo;
    int questionNum;
    MediaPlayer audioQuestion;
    MediaPlayer timerSound;
    AdView mAdView;

    @Override
    public void onResume() {
        super.onResume();
        if (getPurchase("noads")) {
            mAdView.setEnabled(false);
            mAdView.setVisibility(View.GONE);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        timerSound = MediaPlayer.create(MainActivity.this,R.raw.timer_start);
        tvTimer = (TextView) findViewById(R.id.tvTimer);
        tvQuestion = (TextView) findViewById(R.id.tvAnswer);

        pInfo = new PlayerInfo[maxPlayers];

        pInfo[0] = new PlayerInfo((TextView) findViewById(R.id.tvPlayer1), (TextView) findViewById(R.id.tvScore1));
        pInfo[1] = new PlayerInfo((TextView) findViewById(R.id.tvPlayer2), (TextView) findViewById(R.id.tvScore2));

        Count = new CountDownTimer(5000, 10){
            @SuppressLint("DefaultLocale")
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished/1000;
                long milliseconds = millisUntilFinished%1000/10;

                tvTimer.setText(String.format("%02d:%02d", seconds,milliseconds));
            }

            public void onFinish() {
                isRun = false;
                TimerStop();
            }

        };

        NextQuestion();

        btnRunTimer = (ImageView) findViewById(R.id.btnRunTimer);
        btnRunTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isRun) {
                    btnRunTimer.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.pause, null));
                    timerSound.start();
                    Count.start();
                }
                else{
                    Count.cancel();
                    TimerStop();
                }
                isRun = !isRun;
            }
        });

        btnSettings = (ImageView) findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(getApplicationContext(),SettingsActivity.class);
                startActivity(myIntent);
            }
        });

        btnRestart = (ImageView) findViewById(R.id.btnRestart);
        btnRestart.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                Count.cancel();
                tvTimer.setText("05:00");
                activePlayer = 1;
                pInfo[0].Clear();
                pInfo[1].Clear();
                GetQuestions();
                NextQuestion();
            }
        });

        btnAudio = (Button) findViewById(R.id.btnAudio);
        btnAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioQuestion.isPlaying())
                    audioQuestion.stop();
                else
                    audioQuestion.start();
            }
        });
    }

    public static class PlayerInfo{
        TextView tvNickname;
        TextView tvScore;
        int score;

        PlayerInfo(TextView tvNickname, TextView tvScore) {
            this.tvNickname = tvNickname;
            this.tvScore = tvScore;
            score = 0;
        }

        @SuppressLint("DefaultLocale")
        void AddPoint()
        {
            score++;
            tvScore.setText(String.format("%03d", score));
        }

        @SuppressLint("DefaultLocale")
        void Clear()
        {
            score = 0;
            tvScore.setText(String.format("%03d", 0));
        }
    }

    @SuppressLint("SetTextI18n")
    void TimerStop(){
        btnRunTimer.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.play, null));

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogCustom));
        builder.setTitle(pInfo[activePlayer-1].tvNickname.getText()+", время вышло")
                .setMessage("Вы успели ответить?")
                .setCancelable(false)
                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pInfo[activePlayer-1].AddPoint();
                        NextPlayer();
                        NextQuestion();
                        if (secondFail)
                            secondFail = false;
                    }
                })
                .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NextPlayer();
                        if (secondFail){
                            NextQuestion();
                            secondFail = false;
                        }
                        else
                        {
                            secondFail = true;
                        }

                    }
                });

        AlertDialog dialog  = builder.create();
        dialog.show();
        tvTimer.setText("05:00");
    }

    public void NextPlayer(){
        activePlayer++;
        if (activePlayer > maxPlayers)
            activePlayer = 1;
    }

    void NextQuestion() {
        try {
            if (questions.size() == 0)
                GetQuestions();
            Random rnd = new Random();
            questionNum = rnd.nextInt(questions.size());
            tvQuestion.setText(questions.get(questionNum));
            audioQuestion = MediaPlayer.create(MainActivity.this, getResources().getIdentifier("a" + audios.get(questionNum), "raw", getPackageName()));
            questions.remove(questionNum);
            audios.remove(questionNum);
        }
        catch(Exception ex)
        {}
    }

    void GetQuestions(){
        try {
            TinyDB tinydb = new TinyDB(getApplicationContext());
            ArrayList<Boolean> checked = tinydb.getListBoolean("ChoosedQuestions");
            int num = 0;

            XmlPullParser parser = getResources().getXml(R.xml.questions);

            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG
                        && parser.getName().equals("question")) {
                    if (checked.size()==0) {
                        questions.add(parser.getAttributeValue(null, "text"));
                        audios.add(parser.getAttributeValue(null, "audio"));
                    }
                    else if (checked.get(num++)) {
                            questions.add(parser.getAttributeValue(null, "text"));
                            audios.add(parser.getAttributeValue(null, "audio"));
                    }
                }
                parser.next();
            }
        } catch (Throwable t) {
            Toast.makeText(this,
                    "Ошибка при загрузке XML-документа: " + t.toString(), Toast.LENGTH_LONG)
                    .show();
        }
    }

    public Boolean getPurchase(String skuId) {
        SharedPreferences activityPreferences = getSharedPreferences("purchases",MODE_PRIVATE);
        return activityPreferences.getBoolean(skuId,false);
    }
}
