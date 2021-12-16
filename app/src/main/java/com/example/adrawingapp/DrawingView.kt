package com.example.adrawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context:Context, attrs:AttributeSet) : View(context, attrs) {

    private var mDrawPath:CustomPath? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mCanvasPaint: Paint? = null
    private var color:Int = Color.BLACK
    private var mBrushSize:Float = 0.toFloat()
    private var canvas:Canvas? = null

    private var mPaths = ArrayList<CustomPath>()
    private var undoPaths = ArrayList<CustomPath>()

    init {
        setUpDrawingView()
    }

    private fun setUpDrawingView(){
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        color = Color.BLACK
        mBrushSize = 20.toFloat()

        mDrawPaint?.style = Paint.Style.STROKE
        mDrawPaint?.strokeCap = Paint.Cap.ROUND
        mDrawPaint?.strokeJoin = Paint.Join.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //Loading up -> Setting up the canvas to be drawn
        mCanvasBitmap = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        mCanvasBitmap?.let {
            canvas?.drawBitmap(it,0f,0f,mCanvasPaint)
        }

        for(m in mPaths){
            mDrawPaint?.strokeWidth = m.brushThickness
            mDrawPaint?.color = m.color
            canvas?.drawPath(m,mDrawPaint!!)
        }

        if(mDrawPath != null){
            mDrawPaint?.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint?.color = mDrawPath!!.color
            canvas?.drawPath(mDrawPath!!,mDrawPaint!!)
        }


    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when(event.action){
            MotionEvent.ACTION_DOWN -> {
                mDrawPath?.brushThickness = mBrushSize
                mDrawPath?.color = color
                mDrawPath?.reset()
                mDrawPath?.moveTo(touchX, touchY)

            }
            MotionEvent.ACTION_MOVE -> {
                mDrawPath?.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                mPaths.add(mDrawPath!!)
                mDrawPath = CustomPath(color,mBrushSize)
            }
            else -> return false
        }

        invalidate()
        return true

    }

     fun setBrushSize(newSize:Float){
        val brushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
        newSize, resources.displayMetrics)
        mBrushSize = brushSize
         invalidate()
    }

    fun undoDrawing(){
        if(mPaths.isNotEmpty()){
            undoPaths.add(mPaths.removeAt(mPaths.size-1))
            invalidate()
         }
    }

    fun setColor(newColor:String){
        color = Color.parseColor(newColor)
        mDrawPaint?.color = color
    }



    internal inner class CustomPath(var color:Int,
    var brushThickness:Float): Path()
}