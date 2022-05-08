package com.example.capstonandroid.settingViews

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat
import com.example.capstonandroid.Data.Datas
import kotlinx.android.synthetic.main.activity_main.*

/**
 * textureView의 변화를 model에 전달
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
 * **/
class TextureViewSetting(var context: Context) {

    var activity_main : Activity = context as Activity
    lateinit var imageDimension : Size
    lateinit var cameraDevice : CameraDevice
    lateinit var surface : Surface
    lateinit var captureRequestBuilder : CaptureRequest.Builder
    lateinit var cameraCaptureSessions : CameraCaptureSession
    var cameraId = "0"

    fun setTextureView(){
//        activity_main.linearForTextureView.layoutParams.height = activity_main.resources.displayMetrics.heightPixels/2
        activity_main.linearForTextureView.layoutParams.height = activity_main.resources.displayMetrics.widthPixels/4*3
        activity_main.textureView.surfaceTextureListener = textureListener
    }

    private var textureListener: TextureView.SurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, width: Int, height: Int) {
            // 사이즈가 바뀔 때
        }
        override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
            // 이미지가 바뀔때마다 model에 이미지가 바뀌었다는 이벤트를 발생시킨다
            Datas.instance.bitmapSubject.onNext(activity_main.textureView.bitmap)
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
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            // CameraManager 에서 cameraIdList 의 값을 가져온다
            // FaceCamera 값이 true 이면 전면, 아니면 후면 카메라
//            cameraId = "0"

            val characteristics = manager.getCameraCharacteristics(cameraId!!)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            // SurfaceTexture 에 사용할 Size 값을 map 에서 가져와 imageDimension 에 할당해준다
            imageDimension = map!!.getOutputSizes<SurfaceTexture>(SurfaceTexture::class.java)[0]

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                // 카메라 권한이 없는 경우 권한을 요청한다
                ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
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
            var surfaceTexture : SurfaceTexture = activity_main.textureView.surfaceTexture!!

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
}