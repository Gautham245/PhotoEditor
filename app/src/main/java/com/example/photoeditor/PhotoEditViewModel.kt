package com.example.photoeditor

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ThumbnailUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PhotoEditViewModel : ViewModel() {

    var mbitmap :MutableLiveData<Bitmap> = MutableLiveData<Bitmap>()

    fun setBitmap(bitmap: Bitmap){
        mbitmap.value = bitmap
    }

    fun getBitmap():Bitmap = mbitmap.value!!

    fun centerCrop() = ThumbnailUtils.extractThumbnail(
        mbitmap.value,
        getCropDimen(mbitmap.value!!),
        getCropDimen(mbitmap.value!!)
    )

    fun getCropDimen(bitmap: Bitmap): Int {
        if (bitmap.getWidth() >= bitmap.getHeight()) {
            return bitmap.getHeight();
        } else {
            return bitmap.getWidth();
        }
    }

    fun rotateRight(): Bitmap = rotateBitmap( -90f)
    fun rotateLeft(): Bitmap = rotateBitmap(90f)

    fun rotateBitmap(degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(
            mbitmap.value!!, 0, 0, mbitmap.value!!.width, mbitmap.value!!.height, matrix, true
        )
    }

    fun filpImage( xFilp: Float, yFilp: Float) : Bitmap {

        val matrix = Matrix();
        matrix.postScale(xFilp , yFilp, mbitmap.value!!.getWidth() / 2f, mbitmap.value!!.getHeight() / 2f);
        return Bitmap.createBitmap(
            mbitmap.value!!,
            0,
            0,
            mbitmap.value!!.getWidth(),
            mbitmap.value!!.getHeight(),
            matrix,
            true
        );
    }
}