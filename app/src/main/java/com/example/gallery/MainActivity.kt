package com.example.gallery

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.food_entry.view.*
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.provider.MediaStore
import android.content.Intent
import android.database.Cursor
import android.database.MergeCursor
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.support.v4.content.FileProvider
import android.widget.Toast
import java.io.File
import java.io.IOException
import android.support.annotation.RequiresApi
import java.io.FileOutputStream
import java.lang.Exception
import java.net.URI
import java.util.*


class MainActivity : AppCompatActivity() {

    var adapter: FoodAdapter? = null
    var foodsList = ArrayList<Food>()

    private val MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1
    private val CAMERA_REQUEST = 0

    private var mCurrentPhotoPath: String? = null
    private var photoURI: Uri? = null
    val REQUEST_TAKE_PHOTO = 1


    @SuppressLint("InlinedApi", "Recycle")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS
                )
            }
        }

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS
                )
            }
        }


//        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        startActivityForResult(cameraIntent, CAMERA_REQUEST)


        val uriExternal = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uriInternal = android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        val cursorExternal = contentResolver.query(
            uriExternal,
            projection,
            "_data IS NOT NULL) GROUP BY (bucket_display_name",
            null,
            null
        )
        val cursorInternal = contentResolver.query(
            uriInternal,
            projection,
            "_data IS NOT NULL) GROUP BY (bucket_display_name",
            null,
            null
        )
        val cursor = MergeCursor(arrayOf(cursorExternal, cursorInternal))

        while (cursor.moveToNext()) {
            val album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))

            val cursorExternal1 = contentResolver.query(
                uriExternal, projection,
                "bucket_display_name = \"$album\"", null, null
            )
            val cursorInternal1 = contentResolver.query(
                uriInternal, projection,
                "bucket_display_name = \"$album\"", null, null
            )

            val cursor1 = MergeCursor(arrayOf(cursorExternal1, cursorInternal1))
            while (cursor1.moveToNext()) {
                val path = cursor1.getString(cursor1.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))

                foodsList.add((Food(path)))

                break
            }

            break
        }
        
        adapter = FoodAdapter(this, foodsList)

        galleryGridView.adapter = adapter
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @Throws(IOException::class)
    private fun createImageFile(bitmap: Bitmap): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        val fOut = FileOutputStream(image)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut)
        fOut.flush()
        fOut.close()

        mCurrentPhotoPath = image.absolutePath
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
                Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show()
            }

            if (photoFile != null) {
                try {
                    photoURI = FileProvider.getUriForFile(
                        this,
                        "com.example.android.provider",
                        photoFile
                    )
                } catch (ex: Exception) {
                    println("1234567890")
                    println(ex.message)
                }

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFile)
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
            }
        }
    }

    class FoodAdapter(context: Context, var foodsList: ArrayList<Food>) : BaseAdapter() {
        var context: Context? = context

        override fun getCount(): Int {
            return foodsList.size
        }

        override fun getItem(position: Int): Any {
            return foodsList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        @SuppressLint("ViewHolder", "InflateParams")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val food = this.foodsList[position]

            val inflator = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val foodView = inflator.inflate(R.layout.food_entry, null)
            foodView.tvName.setImageURI(Uri.fromFile(File(food.name)))

            return foodView
        }
    }
}
