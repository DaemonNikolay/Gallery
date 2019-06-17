package com.example.gallery

import android.graphics.drawable.Drawable

class Image(pathIntoDevice: String, image: Drawable? = null) {
    var path: String? = pathIntoDevice
    var image: Drawable? = image
}