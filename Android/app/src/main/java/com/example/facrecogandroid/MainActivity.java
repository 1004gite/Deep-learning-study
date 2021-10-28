package com.example.facrecogandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// ./gradlew installDebug 로 실행
public class MainActivity extends AppCompatActivity {

    TextView textResult;
    EditText editTextNum;
    Button btnSetInput;
    Button btnPredict;
    ImageView imageResult;

    String fileName = "";
    Module module;
    Bitmap bitmap;
    static float[] NO_MEAN_GRAY = new float[] {0.0f};
    static float[] NO_STD_GRAY = new float[] {1.0f};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageResult = (ImageView)findViewById(R.id.imageResult);
        textResult = (TextView)findViewById(R.id.textResult);
        textResult.setText("Result");
        editTextNum = (EditText)findViewById(R.id.editTextNum);
        btnSetInput = (Button)findViewById(R.id.btnSetInput);
        btnSetInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileName = "test"+String.valueOf(editTextNum.getText())+".jpg";
                editTextNum.setText("");
                Toast.makeText(getApplicationContext(),"setting Done",Toast.LENGTH_SHORT).show();
                //키보드 내리기에 사용됨
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(editTextNum.getWindowToken(), 0);
            }
        });
        btnPredict = (Button)findViewById(R.id.btnPredict);
        btnPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Predict();
            }
        });

        // 모델 불러오기
        try {
//            module = LiteModuleLoader.load(assetFilePath(this,"androidModel.pt"));
            module = LiteModuleLoader.load(assetFilePath(this,"androidModel.ptl"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void Predict(){
        // 이미지 불러오기
        try {
            bitmap = BitmapFactory.decodeStream(getAssets().open(fileName));
            bitmap = bitmap.createScaledBitmap(bitmap,200,200,true);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),"file error1",Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        //to tensor
        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        // 결과값 예측
        Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
        float[] scores = outputTensor.getDataAsFloatArray();

        float maxScore = -Float.MAX_VALUE;
        int maxScoreIdx = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxScoreIdx = i;
            }
        }
        String resultName;
        if(maxScoreIdx < 10) {
            resultName = "test0" + Integer.toString(maxScoreIdx)+".jpg";
        }
        else{
            resultName = "test" + Integer.toString(maxScoreIdx)+".jpg";
        }
        textResult.setText(resultName);
        try {
            Bitmap tmpBitmap = BitmapFactory.decodeStream(getAssets().open(resultName));
            imageResult.setImageBitmap(tmpBitmap);
            Toast.makeText(getApplicationContext(),"Predicted",Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),"file error2",Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
}