package com.uem.miapp.util

import android.content.Context
import android.graphics.Bitmap
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.*

object GPUImageUtils {

    enum class FilterType {
        NONE, SEPIA, GRAYSCALE, SKETCH, TOON
    }

    fun applyFilter(context: Context, bitmap: Bitmap, filterType: FilterType): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setImage(bitmap)

        val filter = when (filterType) {
            FilterType.SEPIA -> GPUImageSepiaToneFilter()
            FilterType.GRAYSCALE -> GPUImageGrayscaleFilter()
            FilterType.SKETCH -> GPUImageSketchFilter()
            FilterType.TOON -> GPUImageToonFilter()
            FilterType.NONE -> GPUImageFilter() // sin filtro
        }

        gpuImage.setFilter(filter)
        return gpuImage.bitmapWithFilterApplied
    }
}
