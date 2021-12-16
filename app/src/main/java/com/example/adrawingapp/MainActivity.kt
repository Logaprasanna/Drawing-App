package com.example.adrawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private var drawingView:DrawingView? = null
    private var ibBrush:ImageButton? = null
    private var undoButton:ImageButton? = null
    private var externalStorageButton:ImageButton? = null
    private var saveButton:ImageButton? = null
    private var frameLayoutContainer:FrameLayout? = null
    private var progressDialog:Dialog? = null

    private var mCurrentClickedPaintBtn:ImageButton? = null
    private var llPaintColors: LinearLayout? = null

    private var openGalleryLauncher:ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode == RESULT_OK && result?.data!= null){
                val backgroundImage = findViewById<ImageView>(R.id.background_img)
                backgroundImage.setImageURI(result.data?.data)
            }

        }

    private var resultsPermission:ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted){
                    if(permissionName==Manifest.permission.READ_EXTERNAL_STORAGE){
                        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)
                        Toast.makeText(this,
                            "Permission granted to access external storage",
                            Toast.LENGTH_SHORT).show()
                    }
                }else{
                    Toast.makeText(this,
                        "Oops! You just denied permission",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawingView)
        drawingView?.setBrushSize(20.toFloat())

        //The brush button
        ibBrush = findViewById(R.id.ib_brush_size)
        ibBrush?.setOnClickListener{
            showBrushDialog()
        }

        //The undo button
        undoButton = findViewById(R.id.ib_undo)
        undoButton?.setOnClickListener{
            drawingView?.undoDrawing()
        }

        //The save button
        saveButton = findViewById(R.id.ib_save)
        saveButton?.setOnClickListener{

            lifecycleScope.launch{
                if(isReadStorageAllowed()){
                    showProgressDialog()
                    frameLayoutContainer = findViewById(R.id.frame_layout_container)
                    saveFileFromBitmap(getBitmapFromView(frameLayoutContainer!!))
                }
            }


        }

        //The color change when clicked
        llPaintColors = findViewById(R.id.ll_paint_colors)
        mCurrentClickedPaintBtn = llPaintColors!![1] as ImageButton

        mCurrentClickedPaintBtn?.setImageDrawable(
           ContextCompat.getDrawable(this,R.drawable.pallette_pressed)
        )

        //The external storage
        externalStorageButton = findViewById(R.id.external_storage)
        externalStorageButton?.setOnClickListener{
                //TODO: put down the logic here
            if(shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)){
                showRationaleDialogBox("Permission required to access external storage",
                "Permission has been denied")
            }else{
                resultsPermission.launch(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE))
            }


        }

    }

    //And finally, the share feature
    private fun shareImage(result:String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path, uri ->
            var shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "images/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }


    //get the bitmap from the view
    private fun getBitmapFromView(view: View):Bitmap{
        //first, I need to create a bitmap that matches the view
        val theBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        //bind it to the canvas
        var canvas = Canvas(theBitmap)
        //search for any image backgrounds
        var imgBackground = view.background
        if(imgBackground!=null){
            //if it's there then draw onto the canvas
            imgBackground.draw(canvas)
        }else{
            //if it's not there then just draw the white color
            canvas.drawColor(Color.WHITE)
        }
        //is there something that I have to do here
        //draw the final view onto the canvas
        view.draw(canvas)
        //return the bitmap
        return theBitmap
    }

    private suspend fun saveFileFromBitmap(mBitMap:Bitmap):String{
        var result = ""
        withContext(Dispatchers.IO){
            if (mBitMap!=null){
                try {
                    var bytes = ByteArrayOutputStream()
                    mBitMap.compress(Bitmap.CompressFormat.PNG, 90,bytes)

                    //what stops me from writing onto the file
                    var f = File(externalCacheDir?.absoluteFile.toString() + File.separator +
                            "DrawingApp_" + System.currentTimeMillis()/1000 + ".png")

                    var fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread{
                        if (result.isNotEmpty()){
                            dismissProgressDialog()
                            Toast.makeText(this@MainActivity, "File Saved Successfully + $result", Toast.LENGTH_SHORT).show()
                            shareImage(result)

                        } else{
                            dismissProgressDialog()
                            Toast.makeText(this@MainActivity, "Something went wrong while saving file", Toast.LENGTH_SHORT).show()

                        }

                    }

                } catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
        return result
    }




    //Checks if the user has permission to access the external storage files
    private fun isReadStorageAllowed():Boolean{
        val result = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }


     fun paintClicked(view: View){
        if(mCurrentClickedPaintBtn!= view){
            var imageButton = view as ImageButton
            var colorTag = imageButton.tag.toString()

            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallette_pressed)
            )

            mCurrentClickedPaintBtn?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallette_normal)
            )

            mCurrentClickedPaintBtn = view
        }
    }

    private fun showRationaleDialogBox(title:String, message:String){
        var builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){dialog, _->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun showBrushDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.brush_size_picker)
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ib_small)
        smallBtn.setOnClickListener{
            drawingView?.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.ib_medium)
        mediumBtn.setOnClickListener{
            drawingView?.setBrushSize(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ib_large)
        largeBtn.setOnClickListener{
            drawingView?.setBrushSize(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    //show progress dialog
    private fun showProgressDialog(){
        progressDialog = Dialog(this)

        progressDialog?.setContentView(R.layout.progress_dialog_box)

        progressDialog?.show()
    }

    //dismiss progress dialog
    private fun dismissProgressDialog(){
        if (progressDialog!=null){
            progressDialog?.dismiss()

        }
    }

}