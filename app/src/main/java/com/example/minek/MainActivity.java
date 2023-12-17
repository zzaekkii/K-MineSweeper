package com.example.minek;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    static class Pair{
        int x,y;
        Pair(int x,int y){
            this.x = x;
            this.y = y;
        }
    }
    private long backBtnTime = 0;
    private AdView mAdView;
    BlockButton[][] buttons = new BlockButton[9][9];
    boolean modeStatus;
    ToggleButton modeButton;
    private Button restartButton;
    private TextView timeF;
    private int elapsedTimeInSeconds = 0;
    private TextView minesF;
    static int mines = 10;
    TextView gameO;

    ImageView image;
    MusicService mediaPlayer;

    Queue<Pair> q = new LinkedList<>();
    int [] dx = {0,1,1,1,0,-1,-1,-1};
    int [] dy = {-1,-1,0,1,1,1,0,-1};
    int[][] board = new int[9][9]; // 이것은 안 보이는 차원에 있는 투명 지뢰밭
    boolean[][] vis = new boolean[9][9];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(getApplicationContext(), MusicService.class));

        modeButton = findViewById(R.id.mode);
        image = (ImageView)findViewById(R.id.ghost);
        gameO=findViewById(R.id.gameOver);

        // 구글 애드
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        // 구글 애드
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // 흘러가는 시간
        timeF = findViewById(R.id.timeT);

        // 지뢰 개수 (대충 예측 샷임)
        minesF = findViewById(R.id.mineM);
        updateMines();

        restartButton = findViewById(R.id.restart);
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetGame();
                restartButton.setVisibility(View.INVISIBLE);
            }
        });

        for(int i=0;i<9;i++)
            for(int j=0;j<9;j++){
                board[i][j]=0;
                vis[i][j]=false;
            }

        // 1초마다 타이머 업데이트
        new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                elapsedTimeInSeconds++;
                updateTimer();
            }

            @Override
            public void onFinish() {
            }
        }.start();

        // 터치 모드
        modeButton.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        if(isChecked) modeStatus=true;
                        else modeStatus=false;
                    }
                }
        );

        // 지뢰 테이블
        TableLayout table = findViewById(R.id.tableLayout);

        TableRow.LayoutParams layoutParams =
                new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT,
                        1.0f);

        for (int i = 0; i < 9; i++) {
            TableRow tableRow = new TableRow(this);
            table.addView(tableRow);

            for (int j = 0; j < 9; j++) {
                buttons[i][j] = new BlockButton(this,i,j);
                GradientDrawable border = new GradientDrawable();
                border.setColor(0xFF515151);
                border.setStroke(4, 0x080013);
                buttons[i][j].setBackground(border);
                buttons[i][j].setLayoutParams(layoutParams);
                tableRow.addView(buttons[i][j]);
                buttons[i][j].setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        if(modeStatus)((BlockButton)v).breakBlock();
                        else ((BlockButton)v).toggleFlag();
                    }
                }
                );
            }
        }
        setMines(buttons);

        for (int i=0; i<9; i++)
            for(int j=0; j<9; j++){
                if(board[i][j]==99)continue;
                int cnt=0;
                for(int k=0; k<8; k++){
                    int nx = i + dx[k];
                    int ny = j + dy[k];
                    if(nx<0||nx>=9||ny<0||ny>=9)continue;
                    if(board[nx][ny]==99)cnt++;
                }
                board[i][j]=cnt;
                buttons[i][j].neighborMines=cnt;
            }

    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(getApplicationContext(), MusicService.class));
        super.onDestroy();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        stopService(new Intent(getApplicationContext(), MusicService.class));
    }
    @Override
    protected void onPause() {
        super.onPause();
        stopService(new Intent(getApplicationContext(), MusicService.class));
    }
    @Override
    public void onBackPressed() {
        long curTime = System.currentTimeMillis();
        long gapTime = curTime - backBtnTime;

        if (0 <= gapTime && 2000 >= gapTime) {
            super.onBackPressed();
        } else {
            backBtnTime = curTime;
            Toast.makeText(this, "한 번 더 누르면 게임을 종료합니다.", Toast.LENGTH_SHORT).show();
        }
        stopService(new Intent(getApplicationContext(), MusicService.class));
    }

    private void updateTimer() {
        int seconds = elapsedTimeInSeconds;
        String timeString = String.format("%d", seconds);
        timeF.setText(timeString);
    }

    public void updateMines() {
        minesF.setText(String.valueOf(mines));
    }

    private void setMines(BlockButton[][] buttons) {
        Random random = new Random();
        int minesCount = 0;

        while (minesCount < 10) {
            int randomX = random.nextInt(9); // 0~8 랜덤 x좌표
            int randomY = random.nextInt(9); // 0~8 랜덤 y좌표

            if (!buttons[randomX][randomY].isMine()) {
                board[randomX][randomY]=99; // 아무튼 지뢰가 담겨있는 거임
                buttons[randomX][randomY].setMine();
                minesCount++;
            }
        }
    }
    private void resetGame() {
        elapsedTimeInSeconds = 0;
        updateTimer();

        mines = 10; // 지뢰 수 초기화
        updateMines();
        gameO.setText("");

        // 버튼 초기화
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++) {
                board[i][j]=0;
                buttons[i][j].resetBlock();
                buttons[i][j].setEnabled(true);
            }

        setMines(buttons);
        for (int i=0; i<9; i++)
            for(int j=0; j<9; j++){
                if(board[i][j]==99)continue;
                int cnt=0;
                for(int k=0; k<8; k++){
                    int nx = i + dx[k];
                    int ny = j + dy[k];
                    if(nx<0||nx>=9||ny<0||ny>=9)continue;
                    if(board[nx][ny]==99)cnt++;
                }
                board[i][j]=cnt;
                buttons[i][j].neighborMines=cnt;
            }
        restartButton.setVisibility(View.INVISIBLE);
    }
    public class BlockButton extends AppCompatButton {
        private int x;
        private int y;
        private boolean mine;
        private boolean flag;
        private int neighborMines;

        // 생성자
        public BlockButton(Context context, int x, int y) {
            super(context);
            this.x = x;
            this.y = y;
            this.mine = false;
            this.flag = false;
            this.neighborMines = 0;
        }

        public boolean isMine(){
            return mine;
        }

        public void setMine(){
            this.mine=true;
        }

        public void toggleFlag() {
            flag = !flag;
            if (flag) {
                setText("🚩");
                mines--;
            } else {
                setText("");
                mines++;
            }
            updateMines();
        }

        // 블록 열기 메소드
        public void breakBlock() {
            if (!flag) {
                // 이제 클릭 ㄴㄴ
                setClickable(false);

                if (mine) {
                    // 지뢰인 경우
                    GradientDrawable border = new GradientDrawable();
                    border.setColor(0xFF8A0A0A);
                    border.setStroke(4, 0x080013);
                    setBackground(border);
                    setText("💀");
                    gameOver();
                } else {
                    // 지뢰 아닌 경우
                    GradientDrawable border = new GradientDrawable();
                    border.setColor(0xFF8A0A0A);
                    border.setStroke(4, 0x080013);
                    setBackground(border);

                    if(neighborMines==0) setText("");
                    else setText(String.valueOf(neighborMines));

                    if(board[x][y]==0){
                        q.add(new Pair(x,y));
                        while(!q.isEmpty()){
                            Pair p = q.poll();
                            if(vis[p.x][p.y])continue;
                            if(board[p.x][p.y]==99)continue;
                            vis[p.x][p.y]=true;
                            for(int k=0; k<8; k++){
                                int nx = p.x + dx[k];
                                int ny = p.y + dy[k];
                                if(nx<0||nx>=9||ny<0||ny>=9)continue;
                                if(board[nx][ny]==99||vis[nx][ny])continue;
                                buttons[nx][ny].breakBlock();
                                vis[nx][ny]=true;
                                q.add(new Pair(nx,ny));
                            }
                        }
                    }
                    else {
                        vis[x][y]=true;
                    }
                    gameClear();
                }
            }
        }

        public void gameClear() {
            boolean allBlocksOpened = true;

            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
//                    if (!buttons[i][j].isMine() && buttons[i][j].isEnabled()) {
                    if(board[i][j]!=99&&!vis[i][j]){
                        allBlocksOpened = false;
                        break;
                    }
                }
                if (!allBlocksOpened) {
                    break;
                }
            }
            if (allBlocksOpened) {
                for(int i=0;i<9;i++)
                    for(int j=0;j<9;j++)
                        buttons[i][j].setEnabled(false);

                Toast.makeText(MainActivity.this, "Game Cleared!",
                        Toast.LENGTH_SHORT).show();
                gameO.setText(String.valueOf("Game Clear!"));
                restartButton.setVisibility(View.VISIBLE);
            }
        }

        public void gameOver(){
            for(int i=0;i<9;i++)
                for(int j=0;j<9;j++){
                    if(board[i][j]==99)buttons[i][j].setText("💀");
                    buttons[i][j].setEnabled(false);
                }
            if (mediaPlayer != null) {
                mediaPlayer.changeMusic();
            }
            image.setImageResource(R.drawable.ghost_photo);
            image.setVisibility(View.VISIBLE);
            Toast.makeText(MainActivity.this, "Game Over!", Toast.LENGTH_SHORT).show();
            gameO.setText(String.valueOf("Game Over!"));
            restartButton.setVisibility(View.VISIBLE);

            // 한 3초 정도만 보여줌. 7초 내에 사랑에 빠질 수도 있다는 연구가 있음.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    image.setVisibility(View.INVISIBLE);
                }
            }, 3000);
        }
        public void resetBlock() {
            setClickable(true);
            mine = false;
            flag = false;
            neighborMines = 0;

            GradientDrawable border = new GradientDrawable();
            border.setColor(0xFF515151);
            border.setStroke(4, 0x080013);
            setBackground(border);

            setText("");
        }
    }
}