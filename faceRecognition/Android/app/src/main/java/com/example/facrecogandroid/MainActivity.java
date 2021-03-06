package com.example.facrecogandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.util.Log;
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
    ImageView imageInput;

    String fileName = "";
    Module module;
    Bitmap bitmap;
    long[] tensorShape = new long[] {1,1,200,200};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageInput = (ImageView)findViewById(R.id.imageInput);
        imageResult = (ImageView)findViewById(R.id.imageResult);
        textResult = (TextView)findViewById(R.id.textResult);
        textResult.setText("Result");
        editTextNum = (EditText)findViewById(R.id.editTextNum);
        btnSetInput = (Button)findViewById(R.id.btnSetInput);
        btnSetInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String num = String.valueOf(editTextNum.getText());
                if(num.length() == 1){
                    num = "0"+num;
                }
                fileName = "test"+num+".jpg";
                editTextNum.setText("");
                Bitmap tmpBitmap = null;
                try {
                    tmpBitmap = BitmapFactory.decodeStream(getAssets().open(fileName));
                    imageInput.setImageBitmap(tmpBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
//        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
//                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        Tensor inputTensor = Tensor.fromBlob(bitmapToGrayArr(bitmap),tensorShape);
        // 결과값 예측
        Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
        float[] scores = outputTensor.getDataAsFloatArray();

        //////
        for (int i = 0; i < scores.length; i++) {
            Log.wtf(Integer.toString(i),Float.toString(scores[i]));
        }

        float maxScore = -Float.MAX_VALUE;
        int maxScoreIdx = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxScoreIdx = i;
            }
        }
        String resultName;
        if(maxScoreIdx < 9) {
            resultName = "test0" + Integer.toString(maxScoreIdx+1)+".jpg";
        }
        else{
            resultName = "test" + Integer.toString(maxScoreIdx+1)+".jpg";
        }
        textResult.setText(resultName);
        try {
            Bitmap tmpBitmap = BitmapFactory.decodeStream(getAssets().open(resultName));
            imageResult.setImageBitmap(tmpBitmap);
//            imageResult.setImageBitmap(bitmap);
            Toast.makeText(getApplicationContext(),"Predicted",Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),"file error2",Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    private float[] bitmapToGrayArr(final Bitmap bitmap){
        int width, height;
        width = bitmap.getWidth();
        height = bitmap.getHeight();

        float[] result = new float[width*height];

        // color information
        float R, G, B;
        int pixel;

        // scan through all pixels
        int i = 0;
        for (int y = 0; y < width; ++y) {
            for (int x = 0; x < height; ++x) {
                // get pixel color
                pixel = bitmap.getPixel(x, y);
                R = (float) Color.red(pixel);
                G = (float) Color.green(pixel);
                B = (float) Color.blue(pixel);
                float gray = (int)(0.2989f * R + 0.5870f * G + 0.1140f * B);

                result[i] = gray;
                i++;
            }
        }
        return result;

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