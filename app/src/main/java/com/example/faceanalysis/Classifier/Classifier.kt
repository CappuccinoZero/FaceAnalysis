package com.example.faceanalysis.Classifier

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

abstract class Classifier(activity:Activity,device:Device,numThreads:Int) {
    init {
        init(activity,device,numThreads)
    }
    enum class Model{
        FLOAT,
        QUANTIZED,
    }
    enum class Device{
        CPU,
        NNAPI,
        GPU,
    }

    companion object{
        private val DIM_BATCH_SIZE = 1
        private val DIM_PIXEL_SIZE = 3
        class Recognition{
            var id = 0
            var title = ""
            var confidence = 0f
            var location:RectF ?= null
        }
        fun create(activity:Activity,device:Device,numThreads:Int,model:Model):Classifier{
            if (model == Model.FLOAT)
                return ClassifierFloatModel(activity,device,numThreads)
            return ClassifierFloatModel(activity,device,numThreads)
        }
    }

    //存储像素值的缓冲区
    private val pixelCache: IntArray by lazy { IntArray(getImageSizeX()*getImageSizeY()) }
    //tflite 的配置选项
    private var tfliteOptions:Interpreter.Options?=null
    //加载的tflite模型
    private lateinit var tfliteModel:MappedByteBuffer
    //对应的识别标签
    private lateinit var labels:ArrayList<String>
    //tflite用于配置GPU加速的对象
    private var gpuDelegate:GpuDelegate ?= null
    //相当于驱动tflite模型的对象
    protected lateinit var tflite:Interpreter
    //用于保存图片数据
    protected var imgData:ByteBuffer ?= null

    //获取图片x长度
    abstract fun getImageSizeX():Int
    //获取图片y长度
    abstract fun getImageSizeY():Int
    //获取模型路径
    abstract fun getModelPath():String
    //获取标签路径
    abstract fun getLabelPath():String
    //用于存储单个颜色通道的字节数
    protected abstract fun getNumBytesPerChannel():Int
    //将颜色每像素的数据存储进byteBuffer
    protected abstract fun addPixelValue(pixelValue:Int)
    //获取原始标签概率值
    protected abstract fun getProbability(index:Int):Float
    //设置原始标签概率值
    protected abstract fun setProbability(index:Int,value:Float)
    //获取用于显示的标签值
    protected abstract fun getNormalizedProbability(index:Int):Float
    //用于推理图片
    protected abstract fun runInference()
    //获取标签数
    protected fun getLabelsSize() = labels.size

    private fun loadModelFile(activity: Activity):MappedByteBuffer{
        val fileDescriptor = activity.assets.openFd(getModelPath())
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,fileDescriptor.startOffset,fileDescriptor.declaredLength)
    }

    private fun loadModelList(activity: Activity): ArrayList<String> {
        val list = ArrayList<String>()
        val reader = BufferedReader(InputStreamReader(activity.assets.open(getLabelPath())))
        var line:String
        while (true){
            line = reader.readLine()?:break
            list.add(line)
        }
        return list
        //return Util.loadModelList(activity,getLabelPath())
    }

    private fun init(activity:Activity,device:Device,numThreads:Int){
        tfliteModel = loadModelFile(activity)
        when(device){
            //Device.NNAPI->tfliteOptions.setUseNNAPI(true)
            Device.GPU->{
                //gpuDelegate = GpuDelegate()
                //tfliteOptions.addDelegate(gpuDelegate)
            }
        }
        tfliteOptions = Interpreter.Options()
        tfliteOptions!!.setNumThreads(numThreads)
        tflite = Interpreter(tfliteModel,tfliteOptions)
        labels = loadModelList(activity)
        imgData = ByteBuffer.allocateDirect(
            DIM_BATCH_SIZE*getImageSizeX()*getImageSizeY()*getNumBytesPerChannel()*DIM_PIXEL_SIZE
        )
        imgData?.order(ByteOrder.nativeOrder())
    }

    private fun convertBitmapToByteBuffer(bitmap:Bitmap){
        if (imgData==null) return
        imgData?.rewind()
        Log.d("测试",pixelCache.size.toString())
        val bm = Bitmap.createScaledBitmap(bitmap, 224, 224, false)
        bm.getPixels(pixelCache,0,bm.width,0,0,bm.width,bm.height)
        var pixel = 0
        for(i in 0 until getImageSizeX()){
            for(j in 0 until getImageSizeY()){
                addPixelValue(pixelCache[pixel++])
            }
        }
    }

    fun recognizeImage(bitmap: Bitmap):ArrayList<Recognition>{
        convertBitmapToByteBuffer(bitmap)
        runInference()
        val arrays = ArrayList<Recognition>()
        for(i in 0 until labels.size)
            arrays.add(Recognition().also {
                it.id = i
                it.title = labels[i] ?:""
                it.confidence = getNormalizedProbability(i)
            })
        return arrays
    }
}
