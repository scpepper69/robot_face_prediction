package tokyo.scpepper.ai_robot_face_prediction

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_second.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class SecondActivity : AppCompatActivity() {

    lateinit var mAdView : AdView

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val classifier = Classifier(this@SecondActivity)
        imageView.setImageResource(R.drawable.org_robot)

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        //Action for predicting from AI Mmdel
        btnPrediction.setOnClickListener {
            Log.d("TAG",imageView.toString())
            if (imageView != null) {
                val predictResult: TextView = findViewById(R.id.answerText)
                val interpolator = progressBar.getInterpolator()
                progressBar.setInterpolator(interpolator)
                progressBar.setVisibility(View.GONE)
                progressBar.setVisibility(View.VISIBLE)

                val targetImg = getBitmapFromImageView(imageView)
                val handler: Handler = Handler()

                // Start in a background thread
                Thread(Runnable {
                    val (label, accuracy) = classifier.classifyImageFromPath(targetImg)
                    val accuracy_round = Math.round(accuracy * 100)
                    try {
                        Thread.sleep(2000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    // Update progress status
                    handler.post(Runnable {
                        progressBar.setVisibility(View.INVISIBLE)
                        predictResult.text = "$label : $accuracy_round %"
                        Toast.makeText(applicationContext, "Successfully detected", Toast.LENGTH_SHORT).show()
                    })

                }).start() // Start the operation
            }
        }

        //Action for opening library
        btnLaunchLibrary.setOnClickListener {
            val intent: Intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, LIBRARY_REQUEST_CODE)
            Toast.makeText(this, "Open Garally", Toast.LENGTH_SHORT).show()
        }

        //Action for popup information window
        btnLaunchInformation.setOnClickListener {
            // Initialize a new layout inflater instance
            val inflater: LayoutInflater =
                getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            // Inflate a custom view using layout inflater
            val view = inflater.inflate(R.layout.popup_information, null)

            // Initialize a new instance of popup window
            val popupWindow = PopupWindow(
                view, // Custom view to show in popup window
                ConstraintLayout.LayoutParams.WRAP_CONTENT, // Width of popup window
                ConstraintLayout.LayoutParams.WRAP_CONTENT // Window height
            )

            // Set an elevation for the popup window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                popupWindow.elevation = 10.0F
            }

            // If API level 23 or higher then execute the code
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Create a new slide animation for popup window enter transition
                val slideIn = Slide()
                slideIn.slideEdge = Gravity.TOP
                popupWindow.enterTransition = slideIn

                // Slide animation for popup window exit transition
                val slideOut = Slide()
                slideOut.slideEdge = Gravity.RIGHT
                popupWindow.exitTransition = slideOut

            }

            // Get the widgets reference from custom view
            val btnClose = view.findViewById<Button>(R.id.btnClose)

            // Set a click listener for popup's button widget
            btnClose.setOnClickListener {
                // Dismiss the popup window
                popupWindow.dismiss()
            }

            // Set a dismiss listener for popup window
            popupWindow.setOnDismissListener {
                Toast.makeText(applicationContext, "Popup closed", Toast.LENGTH_SHORT).show()
            }

            // Finally, show the popup window on app
            TransitionManager.beginDelayedTransition(main_layout)
            popupWindow.showAtLocation(
                main_layout, // Location to display popup window
                Gravity.CENTER, // Exact position of layout to display popup
                0, // X offset
                0 // Y offset
            )
        }
    }

    private lateinit var path: String

    companion object {
        const val CAMERA_REQUEST_CODE = 1
        const val CAMERA_PERMISSION_REQUEST_CODE = 2
        const val LIBRARY_REQUEST_CODE = 11
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SecondActivity.CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put("_data", path)
            }
            Log.d("TAG",path)
            contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            Log.d("TAG",MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())

            val imageUri = FileProvider.getUriForFile(this, "tokyo.scpepper.ai_robot_face_prediction", File(path))
            val inputStream = FileInputStream(File(path))
            val bitmap = BitmapFactory.decodeStream(inputStream)
            answerText.text = ""
            imageView.setImageURI(imageUri)

            Toast.makeText(applicationContext, "Successfully selected", Toast.LENGTH_SHORT).show()

        }
        if (requestCode == SecondActivity.LIBRARY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            val imageData = data?.data ?: return
            val imagePath = imageData?.path
            val imageUri = data?.data
            Log.d("TAG",imageData.toString())

            try{
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver,imageData)
                answerText.text = ""
                imageView.setImageURI(imageData)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            Toast.makeText(applicationContext, "Successfully selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getBitmapFromImageView(view: ImageView): Bitmap {
        view.getDrawingCache(true)
        return (view.drawable as BitmapDrawable)?.let { it.bitmap }
    }

    override fun onResume() {
        super.onResume()

        btnLaunchCamera.setOnClickListener {
            // check camera app
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(packageManager)?.let {
                if (checkCameraPermission()) {
                    takePicture()
                } else {
                    grantCameraPermission()
                }
            } ?: Toast.makeText(this, "There is no camera application", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createSaveFileUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.JAPAN).format(Date())
        val imageFileName = "temp_" + timeStamp

        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/temp")
        if (!storageDir.exists()) {
            storageDir.mkdir()
            Log.d("TAG","mkdir")
        }
        Log.d("TAG",storageDir.toString())
        val file = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )
        path = file.absolutePath
        Log.d("TAG",path)

        return FileProvider.getUriForFile(this, "tokyo.scpepper.ai_robot_face_prediction", file)
    }

    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            putExtra(MediaStore.EXTRA_OUTPUT, createSaveFileUri())
        }

        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    private fun checkCameraPermission() = PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA)


    private fun grantCameraPermission() =
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE)


    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

        @RequiresApi(Build.VERSION_CODES.KITKAT)
        override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.menu_how_to_use -> {
                // Initialize a new layout inflater instance
                val inflater: LayoutInflater =
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

                // Inflate a custom view using layout inflater
                val view = inflater.inflate(R.layout.popup_how_to_use, null)

                // Initialize a new instance of popup window
                val popupWindow = PopupWindow(
                    view, // Custom view to show in popup window
                    ConstraintLayout.LayoutParams.WRAP_CONTENT, // Width of popup window
                    ConstraintLayout.LayoutParams.WRAP_CONTENT // Window height
                )

                // Set an elevation for the popup window
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    popupWindow.elevation = 10.0F
                }


                // If API level 23 or higher then execute the code
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Create a new slide animation for popup window enter transition
                    val slideIn = Slide()
                    slideIn.slideEdge = Gravity.TOP
                    popupWindow.enterTransition = slideIn

                    // Slide animation for popup window exit transition
                    val slideOut = Slide()
                    slideOut.slideEdge = Gravity.RIGHT
                    popupWindow.exitTransition = slideOut

                }

                // Get the widgets reference from custom view
                val btnClose = view.findViewById<Button>(R.id.btnClose)

                // Set a click listener for popup's button widget
                btnClose.setOnClickListener {
                    // Dismiss the popup window
                    popupWindow.dismiss()
                }

                // Set a dismiss listener for popup window
                popupWindow.setOnDismissListener {
                    Toast.makeText(applicationContext, "Popup closed", Toast.LENGTH_SHORT).show()
                }

                // Finally, show the popup window on app
                TransitionManager.beginDelayedTransition(main_layout)
                popupWindow.showAtLocation(
                    main_layout, // Location to display popup window
                    Gravity.CENTER, // Exact position of layout to display popup
                    0, // X offset
                    0 // Y offset
                )
                true
            }
            R.id.menu_about_us -> {
                // Initialize a new layout inflater instance
                val inflater: LayoutInflater =
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

                // Inflate a custom view using layout inflater
                val view = inflater.inflate(R.layout.popup_about_us, null)

                // Initialize a new instance of popup window
                val popupWindow = PopupWindow(
                    view, // Custom view to show in popup window
                    ConstraintLayout.LayoutParams.WRAP_CONTENT, // Width of popup window
                    ConstraintLayout.LayoutParams.WRAP_CONTENT // Window height
                )

                // Set an elevation for the popup window
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    popupWindow.elevation = 10.0F
                }

                // If API level 23 or higher then execute the code
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Create a new slide animation for popup window enter transition
                    val slideIn = Slide()
                    slideIn.slideEdge = Gravity.TOP
                    popupWindow.enterTransition = slideIn

                    // Slide animation for popup window exit transition
                    val slideOut = Slide()
                    slideOut.slideEdge = Gravity.RIGHT
                    popupWindow.exitTransition = slideOut

                }

                // Get the widgets reference from custom view
                val btnClose = view.findViewById<Button>(R.id.btnClose)

                // Set a click listener for popup's button widget
                btnClose.setOnClickListener {
                    // Dismiss the popup window
                    popupWindow.dismiss()
                }

                // Set a dismiss listener for popup window
                popupWindow.setOnDismissListener {
                    Toast.makeText(applicationContext, "Popup closed", Toast.LENGTH_SHORT).show()
                }

                // Finally, show the popup window on app
                TransitionManager.beginDelayedTransition(main_layout)
                popupWindow.showAtLocation(
                    main_layout, // Location to display popup window
                    Gravity.CENTER, // Exact position of layout to display popup
                    0, // X offset
                    0 // Y offset
                )
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

}
