package com.example.capstonandroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.capstonandroid.model.Datas
import com.example.capstonandroid.view.TextureViewSetting
import com.example.capstonandroid.viewModel.ComputeData
import org.pytorch.Module

/*
로딩 화면에서 권한 확인하도록 변경해야함
* */

class MainActivity : AppCompatActivity() {

    lateinit var computeData: ComputeData
    lateinit var textureViewSetting: TextureViewSetting

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //datas instance생성
        Datas()
        Datas.instance.bindSubjects()

        computeData = ComputeData(this)
        computeData.bindRx()

        textureViewSetting = TextureViewSetting(this)
        textureViewSetting.setTextureView()
    }

}