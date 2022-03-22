package com.example.capstonandroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import org.pytorch.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/*
로딩 화면에서 권한 확인하도록 변경해야함
* */

/**
 * TextureView는 SurfaceTexture에 그려진 그림을 나타냄리
 * SurfaceTexture의 surface는 session의 data를 반영함
 * SurfaceTextureListener로 후처
 *
 * 동작 순서 정리
 * textureSurfaceView 준비됨
 * 카메라 manager를 이용하여 surface정보에 맞게 카메라를 연다.
 * 이떄 createCameraPreviewSession 함수에서 session을 만들고 원하는 surfaceTextuer의 surface를 target으로 추가
 *
 * builder가 session에 data를 뿌려줌 -> surface가 session에서 오는 데이터를 적용
 *
 * 이미지를 따오는 방안
 * 1. imageReader의 surface를 target에 추가하여 imageReader에서 추출
 * 2. textureView에서 bitmap을 따옴 -> 어차피 preview를 보여주는과정에서 textuerview는 필요 -> 2번으로 결정
 * **/

class MainActivity : AppCompatActivity() {

    var subject : PublishSubject<Int> = PublishSubject.create()
    var check : Boolean = true
    var changeText : PublishSubject<String> = PublishSubject.create()

    lateinit var module : Module
    var bitmapArr : ArrayList<Bitmap> = ArrayList()
    var tmpBitmap : Bitmap = Bitmap.createBitmap(10,10,Bitmap.Config.ALPHA_8)
    var densityBitmaps : ArrayList<Bitmap> = ArrayList()

    lateinit var imageDimension : Size
    lateinit var cameraDevice : CameraDevice
    lateinit var surfaceTexture : SurfaceTexture
    lateinit var surface : Surface
    lateinit var captureRequestBuilder : CaptureRequest.Builder
    lateinit var cameraCaptureSessions : CameraCaptureSession
    var cameraId = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setMoudle()
        bindRx()
        setTextureView()
    }
    fun setMoudle(){
        module = LiteModuleLoader.load(assetFilePath(this, "fcn_rlstm2_android.ptl"))
        Log.e("모델","모델 로딩")
    }
    fun bindRx(){
        subject
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .filter { check }
            .delay(1000, TimeUnit.NANOSECONDS)
            .subscribeBy(
                onNext = {
                    if(bitmapArr.size < 5) {
                        var bitmap: Bitmap = textureView.bitmap!!
                        bitmap = Bitmap.createScaledBitmap(bitmap, 120, 160, true)
//                        bitmap = Bitmap.createScaledBitmap(bitmap, 640, 480, true)
                        bitmapArr.add(bitmap)
                        Log.e("subject","비트맵 추가")
                    }
                    if(bitmapArr.size >= 5) {
                        check = false
                        var Farr =  bitmapToRGBArr(bitmapArr)
                        var result = getModelResult(Farr)
                        changeText.onNext(result)
                        Log.e("subject","계산 완료")
                    }
                },
                onComplete = {},
                onError = {Log.e("에러_subject",it.message.toString())}
            )

        changeText
            .subscribeOn(Schedulers.io()) // 데이터 흐름을 발생시키는데 (내려가며 읽는 동작) 영향
            .observeOn(AndroidSchedulers.mainThread()) // observer에게 데이터를 보낼 때 영향
            .subscribeBy (
                onNext = {
                    resultTextView.text = it

                    image_density1.setImageBitmap(densityBitmaps[0])
                    image_density2.setImageBitmap(densityBitmaps[1])
                    image_density3.setImageBitmap(densityBitmaps[2])
                    image_density4.setImageBitmap(densityBitmaps[3])
                    image_density5.setImageBitmap(densityBitmaps[4])

                    testImage.setImageBitmap(tmpBitmap)
                    check = true
                    Log.e("UI","UI 적용")
                         },
                onComplete = {},
                onError = {
                    Log.e("changeText 에러", it.message.toString())
                }
            )
    }
    fun getModelResult(floatArr : FloatArray) : String{
        // preparing input tensor
//        var inputTensor : Tensor = TensorImageUtils.bitmapToFloat32Tensor(
//            bitmap,
//            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
//            TensorImageUtils.TORCHVISION_NORM_STD_RGB,
//            MemoryFormat.CHANNELS_LAST)
        var shape : LongArray = LongArray(5).also {
            it[0] = 5L
            it[1] = 1L
            it[2] = 3L
            it[3] = 120L
            it[4] = 160L
        }
//        var shape : LongArray = LongArray(4).also {
//            it[0] = 1L
//            it[1] = 3L
//            it[2] = 640L
//            it[3] = 480L
//        }
        var inputTensor : Tensor = Tensor.fromBlob(floatArr, shape)
//        Log.e("shape", inputTensor.shape().contentToString())

        // running the model
        var output = module.forward(IValue.from(inputTensor)).toTuple()
//         output[0].toTensor().dataAsFloatArray -> densitymap
//        output[1].toTensor().dataAsFloatArray -> result
//        Log.e("output[0]","size: "+output[0].toTensor().dataAsFloatArray.size.toString())
//        Log.e("output[1]",output[1].toTensor().dtype().toString())

        // 어떤 이미지가 input으로 들어갔는지
        tmpBitmap =  Bitmap.createScaledBitmap(bitmapArr[0],450,600,true)

        // densitymap 5개 세팅
        setDensityBitmap(output[0].toTensor().dataAsFloatArray)

        bitmapArr.clear()

        return output[1].toTensor().dataAsFloatArray[4].toString()
    }

    fun setTextureView(){
        linearForTextureView.layoutParams.height = resources.displayMetrics.heightPixels/2

        textureView.surfaceTextureListener = textureListener
    }

    private var textureListener: TextureView.SurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, width: Int, height: Int) {
            // 사이즈가 바뀔 때
        }
        override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
            // 이미지가 바뀔때마다 bitmap으로 따옴
//            Log.e("화면 Tensor", bitmapToRGBTensor(textureView.bitmap!!)[0].size.toString())
            subject.onNext(1)
        }
        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
            // 지정된 SurfaceTexture 를 파괴하고자 할 때 호출된다
            // true 를 반환하면 메서드를 호출한 후 SurfaceTexture 에서 랜더링이 발생하지 않는다
            // 대부분의 응용프로그램은 true 를 반환한다
            // false 를 반환하면 SurfaceTexture#release() 를 호출해야 한다
            return false
        }
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture, width: Int, height: Int) {
            // TextureListener 에서 SurfaceTexture 가 사용가능한 경우, openCamera() 메서드를 호출한다
            openCamera()
        }
    }

    // openCamera() 메서드는 TextureListener 에서 SurfaceTexture 가 사용 가능하다고 판단했을 시 실행된다
    private fun openCamera() {
        // 카메라의 정보를 가져와서 cameraId 와 imageDimension 에 값을 할당하고, 카메라를 열어야 하기 때문에
        // CameraManager 객체를 가져온다
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            // CameraManager 에서 cameraIdList 의 값을 가져온다
            // FaceCamera 값이 true 이면 전면, 아니면 후면 카메라
//            cameraId = "0"

            val characteristics = manager.getCameraCharacteristics(cameraId!!)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            // SurfaceTexture 에 사용할 Size 값을 map 에서 가져와 imageDimension 에 할당해준다
            imageDimension = map!!.getOutputSizes<SurfaceTexture>(SurfaceTexture::class.java)[0]

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                // 카메라 권한이 없는 경우 권한을 요청한다
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
                return
            }

            // CameraManager.openCamera() 메서드를 이용해 인자로 넘겨준 cameraId 의 카메라를 실행한다
            // 이때, stateCallback 은 카메라를 실행할때 호출되는 콜백메서드이며, cameraDevice 에 값을 할달해주고, 카메라 미리보기를 생성한다
            manager.openCamera(cameraId!!, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    // openCamera() 메서드에서 CameraManager.openCamera() 를 실행할때 인자로 넘겨주어야하는 콜백메서드
    // 카메라가 제대로 열렸으면, cameraDevice 에 값을 할당해주고, 카메라 미리보기를 생성한다
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            // MainActivity 의 cameraDevice 에 값을 할당해주고, 카메라 미리보기를 시작한다
            // 나중에 cameraDevice 리소스를 해지할때 해당 cameraDevice 객체의 참조가 필요하므로,
            // 인자로 들어온 camera 값을 전역변수 cameraDevice 에 넣어 준다
            cameraDevice = camera

            // createCameraPreview() 메서드로 카메라 미리보기를 생성해준다
            createCameraPreviewSession()
        }
        override fun onDisconnected(camera: CameraDevice) {
            // 연결이 해제되면 cameraDevice 를 닫아준다
            cameraDevice!!.close()
        }
        override fun onError(camera: CameraDevice, error: Int) {
            // 에러가 뜨면, cameraDevice 를 닫고, 전역변수 cameraDevice 에 null 값을 할당해 준다
            cameraDevice!!.close()
        }
    }
    // openCamera() 에 넘겨주는 stateCallback 에서 카메라가 제대로 연결되었으면
    // createCameraPreviewSession() 메서드를 호출해서 카메라 미리보기를 만들어준다
    private fun createCameraPreviewSession() {
        try {

            // 캡쳐세션을 만들기 전에 프리뷰를 위한 Surface 를 준비한다
            // 레이아웃에 선언된 textureView 로부터 surfaceTexture 를 얻을 수 있다
            surfaceTexture = textureView.surfaceTexture!!

            // 미리보기를 위한 Surface 기본 버퍼의 크기는 카메라 미리보기크기로 구성
            surfaceTexture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
//            surfaceTexture.setDefaultBufferSize(200, 200)


            // 미리보기를 시작하기 위해 필요한 출력표면인 surface
            surface = Surface(surfaceTexture)

            // 미리보기 화면을 요청하는 RequestBuilder 를 만들어준다.
            // 이 요청은 위에서 만든 surface 를 타겟으로 한다
            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder.addTarget(surface)

//            // 이미지 캡쳐를 위한 ImageReader생성 및 타겟 추가
//            var reader : ImageReader = ImageReader.newInstance(imageDimension!!.width, imageDimension!!.height, ImageFormat.JPEG, 1)
//            captureRequestBuilder.addTarget(reader.surface)

            // 위에서 만든 surface 에 미리보기를 보여주기 위해 createCaptureSession() 메서드를 시작한다
            // createCaptureSession 의 콜백메서드를 통해 onConfigured 상태가 확인되면
            // CameraCaptureSession 을 통해 미리보기를 보여주기 시작한다
            cameraDevice!!.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(session: CameraCaptureSession) {
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    if(cameraDevice == null) {
                        // 카메라가 이미 닫혀있는경우, 열려있지 않은 경우
                        return
                    }
                    // session 이 준비가 완료되면, 미리보기를 화면에 뿌려주기 시작한다
                    cameraCaptureSessions = session

                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

                    try {
                        cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

            }, null)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun bitmapToRGBArr(bitmapArray : ArrayList<Bitmap>) : FloatArray{
        var batchsize = bitmapArray.size
        var width = bitmapArray[0].width
        var height = bitmapArray[0].height
        var bitmapSize = height*width
        var arr : FloatArray = FloatArray(height * width * 3 * batchsize) {0f}
        // 열(가로)을 하나씩 본다
        for( batch in 0 until batchsize) {
            for (j in 0 until height) {
                for (i in 0 until width) {
                    var p = bitmapArray[batch].getPixel(i, j)

                    var R = (p and 0xff0000) shr 16
                    var G = (p and 0x00ff00) shr 8
                    var B = (p and 0x0000ff) shr 0

                    var index = j * width + i
                    arr[index] = R.toFloat()
                    arr[bitmapSize+index] = G.toFloat()
                    arr[(bitmapSize*2)+index] = B.toFloat()
                }
            }
        }

        return arr
    }

    fun setDensityBitmap(floatArr : FloatArray){
        densityBitmaps.clear()
        var intArray : IntArray = IntArray(floatArr.size/5)
        var size = floatArr.size/5
        for(count in 0 until 5) {
            for (i in 0 until size) {
                intArray[i] = floatArr[size*count + i].toInt()
            }
            var tmp = Bitmap.createBitmap(120,160,Bitmap.Config.ALPHA_8)
            tmp.setPixels(intArray,0,120,0,0,120,160)
            tmp = Bitmap.createScaledBitmap(tmp, 450, 600, true)
            densityBitmaps.add(tmp)
        }
    }

    fun assetFilePath(context : Context, assetName : String) : String {
        var file: File = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        var iStream: InputStream = context.assets.open(assetName)
        var oStream: OutputStream = FileOutputStream(file)

        var buffer: ByteArray = ByteArray(4 * 1024)
        var read: Int
        while (true) {
            read = iStream.read(buffer)
            if (read == -1) break
            oStream.write(buffer, 0, read)
        }
        oStream.flush()

        return file.absolutePath
    }
}