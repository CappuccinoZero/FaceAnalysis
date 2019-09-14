package com.example.faceanalysis.Classifier

import android.app.Activity


class ClassifierQuantizedModel(activity: Activity, device:Device, numThreads:Int):Classifier(activity,device,numThreads) {
    private var labelProbArray = Array(1){ByteArray(getLabelsSize())}
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
        imgData?.put((pixelValue shr 16 and 0xFF) as Byte)
        imgData?.put((pixelValue shr 8 and 0xFF) as Byte)
        imgData?.put((pixelValue and 0xFF) as Byte)
    }

    override fun getProbability(index: Int): Float {
        return labelProbArray[0][index] as Float
    }

    override fun setProbability(index: Int, value: Float) {
        labelProbArray[0][index] = value as Byte
    }

    override fun getNormalizedProbability(index: Int): Float {
        return ((labelProbArray[0][index] as Int) and  0xff) / 255.0f
    }

    override fun runInference() {
        tflite.run(imgData!!,labelProbArray)
    }
}