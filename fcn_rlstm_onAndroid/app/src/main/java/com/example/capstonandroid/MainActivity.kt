package com.example.capstonandroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.capstonandroid.model.Datas
import com.example.capstonandroid.view.BindViews
import com.example.capstonandroid.view.TextureViewSetting
import com.example.capstonandroid.viewModel.ComputeData
import com.example.capstonandroid.viewModel.Designs
import org.pytorch.Module

/*
로딩 화면에서 권한 확인하도록 변경해야함
* */

class MainActivity : AppCompatActivity() {

    lateinit var computeData: ComputeData
    lateinit var bindViews: BindViews
    lateinit var designs: Designs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //datas instance생성
        Datas()
        Datas.instance.bindSubjects()

        computeData = ComputeData(this)
        computeData.bindRx()

        designs = Designs(this)
        designs.setDesigns()

        bindViews = BindViews(this)
        bindViews.bindViews()
    }

}