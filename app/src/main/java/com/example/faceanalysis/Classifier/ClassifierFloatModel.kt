package com.example.faceanalysis.Classifier

import android.app.Activity



//浮点模型
class ClassifierFloatModel(activity: Activity, device:Device, numThreads:Int):Classifier(activity,device,numThreads) {
    private var labelProbArray = Array(1){FloatArray(getLabelsSize())}
    private val IMG_MEAN = 127.5f
    private val IMG_STD = 127.5f
    override fun getImageSizeX(): Int {
        return 224
    }

    override fun getImageSizeY(): Int {
        return 224
    }

    override fun getModelPath(): String {
        return "mobilenet_v2_1.4_224.tflite"
    }

    override fun getLabelPath(): String {
        return "label.txt"
    }

    override fun getNumBytesPerChannel(): Int {
        return 4
    }

    override fun addPixelValue(pixelValue: Int) {
        imgData?.putFloat(((pixelValue shr 16 and 0xFF) - IMG_MEAN) / IMG_STD)
        imgData?.putFloat(((pixelValue shr 8 and 0xFF) - IMG_MEAN) / IMG_STD)
        imgData?.putFloat(((pixelValue and 0xFF) - IMG_MEAN) / IMG_STD)
    }

    override fun getProbability(index: Int): Float {
        return labelProbArray[0][index]
    }

    override fun setProbability(index: Int, value: Float) {
        labelProbArray[0][index] = value
    }

    override fun getNormalizedProbability(index: Int): Float {
        return labelProbArray[0][index]
    }

    override fun runInference() {
        tflite.run(imgData!!,labelProbArray)
    }

}