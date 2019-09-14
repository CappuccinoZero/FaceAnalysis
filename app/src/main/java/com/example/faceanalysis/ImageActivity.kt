package com.example.faceanalysis

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import com.example.faceanalysis.Classifier.Classifier
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureMimeType
import kotlinx.android.synthetic.main.activity_image.*
import java.io.File

class ImageActivity : AppCompatActivity() {
    private lateinit var detector:FirebaseVisionObjectDetector
    private lateinit var singleDetector:FirebaseVisionObjectDetector
    private lateinit var classifier: Classifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        classifier = Classifier.create(this,Classifier.Device.GPU,4,Classifier.Model.FLOAT)

        usePhoto.setOnClickListener {
            usePhoto()
        }
        initFireBase()
        imageDetector.setOnClickListener {
            usePhoto(1001)
        }

        imageDetectors.setOnClickListener {
            usePhoto(1002)
        }
        tfliteButton.setOnClickListener {
            usePhoto(1003)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val media = PictureSelector.obtainMultipleResult(data)[0]
        val path = media.path ?: ""
        if(requestCode == 1000){
            val image = FirebaseVisionImage.fromFilePath(this, File(path).toUri())

            val labelerOption = FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder()
                .setLocalModelName("model.tflite")
                .setConfidenceThreshold(0.35f)
                .build()
            val labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(labelerOption)
            labeler.processImage(image)
                .addOnSuccessListener {
                for(label in it){
                    textContent.text = if(label.text=="Anime") "动漫" else "手办" + "  概率"+label.confidence
                }
            }
                .addOnFailureListener{
                    Log.d("失败：",it.message)
                }
        }else if(requestCode == 1001){
            val image = FirebaseVisionImage.fromFilePath(this,File(path).toUri())
            singleDetector.processImage(image)
                .addOnSuccessListener {
                    Log.d("测试：","??")
                    val strBf = StringBuffer()
                    Log.d("测试：","???${it.size}")
                    for (obj in it){
                        Log.d("测试：","--")
                        strBf.append("${obj.trackingId}\ntop:${obj.boundingBox.top}\nbottom:${obj.boundingBox.bottom}\nleft:${obj.boundingBox.left}\nright:${obj.boundingBox.right}\nname:${obj.classificationCategory}\nconfidence:${obj.classificationConfidence}\n")
                    }
                    textContent.text = strBf.toString()
                }
                .addOnFailureListener{
                    Log.d("失败：",it.message)
                }
        }else if(requestCode == 1002){
            val image = FirebaseVisionImage.fromFilePath(this,File(path).toUri())
            detector.processImage(image)
                .addOnSuccessListener {
                    val strBf = StringBuffer()
                    for (obj in it){
                        strBf.append("${obj.trackingId}\ntop:${obj.boundingBox.top}\nbottom:${obj.boundingBox.bottom}\nleft:${obj.boundingBox.left}\nright:${obj.boundingBox.right}\nname:${obj.classificationCategory}\nconfidence:${obj.classificationConfidence}\n")
                    }
                    Log.d("成功：",strBf.toString())
                    textContent.text = strBf.toString()
                }
                .addOnFailureListener{
                    Log.d("失败：",it.message)
                }
        }else if(requestCode == 1003){
            val arr = classifier.recognizeImage(getScaleBitmap(path,this))
            var max = arr[0].confidence
            var index = 0
            for(a in arr){
                if(a.confidence>max){
                    max = a.confidence
                    index = a.id
                }
            }
            val a:Int = (arr[index].confidence*100).toInt()
            Log.d("测试",arr[index].title+"  "+arr[index].confidence)
        }
    }

    private fun usePhoto(id:Int = 1000) {
        PictureSelector.create(this).openGallery(PictureMimeType.ofImage()).forResult(id)
    }

    private fun initFireBase(){
        //配置本地模型
        val localModel = FirebaseLocalModel.Builder("model.tflite")
            .setAssetFilePath("manifest.json")
            .build()
        FirebaseModelManager.getInstance().registerLocalModel(localModel)

        //配置云模型
        val conditions = FirebaseModelDownloadConditions.Builder()
            .requireWifi()
            .build()
        val remoteModel = FirebaseRemoteModel.Builder("Test_Remote")
            .enableModelUpdates(true)
            .setUpdatesDownloadConditions(conditions)
            .setUpdatesDownloadConditions(conditions)
            .build()
        FirebaseModelManager.getInstance().registerRemoteModel(remoteModel)
        //用于检查是否已经下载完成
        //下载完成后会回调
        //经测试，下载需要翻墙
        FirebaseModelManager.getInstance().downloadRemoteModelIfNeeded(remoteModel)
            .addOnSuccessListener {
                Log.d("云模型下载","成功")
            }
            .addOnFailureListener{
                Log.d("云模型下载","失败")
            }

        val options = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableClassification()//粗略分类
            .enableMultipleObjects()//多个物体检测
            .build()
        val singleOptions = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .build()

        detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)
        singleDetector = FirebaseVision.getInstance().getOnDeviceObjectDetector(singleOptions)
    }

    fun getScaleBitmap(path: String, context: Context): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        val width = options.outWidth
        val height = options.outHeight
        val mWidth = 500
        val mHeight = 500
        options.inSampleSize = 1
        while (width / options.inSampleSize > mWidth || height / options.inSampleSize > mHeight) {
            options.inSampleSize *= 2
        }
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }
}
