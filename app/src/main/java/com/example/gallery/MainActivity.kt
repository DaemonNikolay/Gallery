package com.example.gallery

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MergeCursor
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v4.app.ActivityCompat.startIntentSenderForResult
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.getSystemService
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.image_entry.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    private var adapter: ImageAdapter? = null

    private val _PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 1
    private val _REQUEST_TAKE_PHOTO = 1


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissions.forEachIndexed { index, it ->
            if (it.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[index] == 0) {
                val imageList = this.generateImageList()
                adapter = ImageAdapter(this, imageList)
                galleryGridView.adapter = adapter

            } else if (it.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[index] == -1) {
                toast("Разрешения на доступ к файлам нет - галерея не фурычит...")
            }
        }
    }

    fun Context.toast(message: CharSequence) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    @SuppressLint("InlinedApi", "Recycle")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    _PERMISSIONS_REQUEST_EXTERNAL_STORAGE
                )
            }
        } else {
            Log.d("", "4444444444444444444")
        }
    }

    private fun generateImageList(): ArrayList<Image> {
        val uriExternal = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uriInternal = MediaStore.Images.Media.INTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        val cursorExternal = this.createCursorExternal(uriExternal, projection)
        val cursorInternal = this.createCursorInternal(uriInternal, projection)
        val cursorAlbum = MergeCursor(arrayOf(cursorExternal, cursorInternal))

        val imageList = ArrayList<Image>()
        while (cursorAlbum.moveToNext()) {
            val album =
                cursorAlbum.getString(cursorAlbum.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))

            val cursorExternal1 = contentResolver.query(
                uriExternal, projection,
                "bucket_display_name = \"$album\"", null, null
            )
            val cursorInternal1 = contentResolver.query(
                uriInternal, projection,
                "bucket_display_name = \"$album\"", null, null
            )

            val cursorImage = MergeCursor(arrayOf(cursorExternal1, cursorInternal1))
            while (cursorImage.moveToNext()) {
                val path = cursorImage.getString(cursorImage.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                imageList.add(Image(path))

                // todo: нужно брать изображения не из общей галереи, а из кастомной директории, куда будут лететь фотографии
                break
            }

            val iconPlus = resources.getDrawable(R.drawable.icon_plus)

            imageList.add(Image("", iconPlus))

            break
        }

        return imageList
    }

    private fun createCursorInternal(uriInternal: Uri, projection: Array<String>): Cursor? {
        return contentResolver.query(
            uriInternal,
            projection,
            "_data IS NOT NULL) GROUP BY (bucket_display_name",
            null,
            null
        )
    }

    private fun createCursorExternal(uriInternal: Uri, projection: Array<String>): Cursor? {
        return contentResolver.query(
            uriInternal,
            projection,
            "_data IS NOT NULL) GROUP BY (bucket_display_name",
            null,
            null
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @Throws(IOException::class)
    private fun createImageFile(bitmap: Bitmap): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "jpeg_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        val fileOut = FileOutputStream(image)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOut)
        fileOut.flush()
        fileOut.close()

        return image
    }


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoBitmap = data!!.extras!!.get("data") as Bitmap

        if (takePictureIntent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = this.createImageFile(photoBitmap)
            } catch (ex: IOException) {
                Toast.makeText(this, "Error! ${ex.message}", Toast.LENGTH_SHORT).show()
            }

            if (photoFile != null) {
                try {
                    var photoURI = FileProvider.getUriForFile(
                        this,
                        "com.example.android.provider",
                        photoFile
                    )
                } catch (ex: Exception) {
                    println(ex.message)
                }

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFile)
                startActivityForResult(takePictureIntent, _REQUEST_TAKE_PHOTO)
            }
        }
    }

    class ImageAdapter(context: Context, private var imagesList: ArrayList<Image>) : BaseAdapter() {
        private var _context: Context? = context
        private val _CAMERA_REQUEST = 1

        override fun getCount(): Int {
            return imagesList.size
        }

        override fun getItem(position: Int): Any {
            return imagesList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        @SuppressLint("ViewHolder", "InflateParams")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val inflator = _context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val imageView = inflator.inflate(R.layout.image_entry, null)

            if (this.imagesList[position].image != null) {
                imageView.path.setImageDrawable(this.imagesList[position].image)
                imageView.setOnClickListener {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//                    startIntentSenderForResult()
//                    startActivityForResult(cameraIntent, this._CAMERA_REQUEST)
                }
            } else {
                val image = this.imagesList[position]
                imageView.path.setImageURI(Uri.fromFile(File(image.path)))
                imageView.setOnClickListener {
                    val dialog = Dialog(this._context)
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                    dialog.setCancelable(false)
                    dialog.setContentView(R.layout.image_view)

                    val popupImage = dialog.findViewById<View>(R.id.image) as ImageView
                    popupImage.setImageURI(Uri.fromFile(File(image.path)))

                    val buttonCancel = dialog.findViewById<View>(R.id.button_cancel) as Button
                    buttonCancel.setOnClickListener {
                        dialog.dismiss()
                    }

                    dialog.show()
                }
            }

            return imageView
        }
    }
}
