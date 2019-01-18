// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.samples.apps.mlkit.facedetection;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.face.Face;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.samples.apps.mlkit.GraphicOverlay;
import com.google.firebase.samples.apps.mlkit.GraphicOverlay.Graphic;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */

public class FaceGraphic extends Graphic {
  private static final float FACE_POSITION_RADIUS = 10.0f;
  private static final float ID_TEXT_SIZE = 40.0f;
  private static final float ID_Y_OFFSET = 50.0f;
  private static final float ID_X_OFFSET = -50.0f;
  private static final float BOX_STROKE_WIDTH = 5.0f;

  private static float blink= 0;

  private static final int[] COLOR_CHOICES = {
    Color.BLUE //, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.RED, Color.WHITE, Color.YELLOW
  };
  private static int currentColorIndex = 0;

  private int facing;

  private final Paint facePositionPaint;
  private final Paint idPaint;
  private final Paint boxPaint;


  private volatile FirebaseVisionFace firebaseVisionFace;

  public FaceGraphic(GraphicOverlay overlay) {
    super(overlay);

    currentColorIndex = (currentColorIndex + 1) % COLOR_CHOICES.length;
    final int selectedColor = COLOR_CHOICES[currentColorIndex];

    facePositionPaint = new Paint();
    facePositionPaint.setColor(selectedColor);

    idPaint = new Paint();
    idPaint.setColor(selectedColor);
    idPaint.setTextSize(ID_TEXT_SIZE);

    boxPaint = new Paint();
    boxPaint.setColor(selectedColor);
    boxPaint.setStyle(Paint.Style.STROKE);
    boxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
  }

  public void updateFace(FirebaseVisionFace face, int facing) {
    firebaseVisionFace = face;
    this.facing = facing;
    postInvalidate();
  }

  private final float OPEN_THRESHOLD = 0.85f;
  private final float CLOSE_THRESHOLD = 0.40f;

  private static int state = 0;

  @Override
  public void draw(Canvas canvas) {
    FirebaseVisionFace face = firebaseVisionFace;
    if (face == null) {
      return;
    }

    // Draws a circle at the position of the detected face, with the face's track id below.
    float x = translateX(face.getBoundingBox().centerX());
    float y = translateY(face.getBoundingBox().centerY());
    canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint);
    canvas.drawText("", x + ID_X_OFFSET, y + ID_Y_OFFSET, idPaint);

    float left = face.getLeftEyeOpenProbability();
    float right = face.getRightEyeOpenProbability();
//    if ((left == Face.UNCOMPUTED_PROBABILITY) ||
//            (right == Face.UNCOMPUTED_PROBABILITY)) {
//      // At least one of the eyes was not detected.
//      return;
//    }

    int flag=0;
    switch (state) {

      case 0:
        if ((left > OPEN_THRESHOLD) && (right > OPEN_THRESHOLD)) {
          // Both eyes are initially open
          Log.i("BlinkTracker", "both open");
          state = 1;
        }
        break;

      case 1:
        if ((left < CLOSE_THRESHOLD) && (right < CLOSE_THRESHOLD)) {
          // Both eyes become closed
          Log.i("BlinkTracker", "both eyes closed");
          state = 2;
        }
        break;

      case 2:
        if ((left > OPEN_THRESHOLD) && (right > OPEN_THRESHOLD)) {
          // Both eyes are open again
          Log.i("BlinkTracker", "blink occurred!");

        //  state = 0;
          state=0;
          flag=2;

        }
        break;
    }

    if(flag==2 && state==0)
    { canvas.drawText(
              "blinks: " + String.format("%.2f",blink++),
              x + ID_X_OFFSET * 3,
              y - ID_Y_OFFSET,
              idPaint); }

    if ((left == Face.UNCOMPUTED_PROBABILITY) ||
            (right == Face.UNCOMPUTED_PROBABILITY)) {
      // At least one of the eyes was not detected.
      return;
    }

//    canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint);
//    canvas.drawText("", x + ID_X_OFFSET, y + ID_Y_OFFSET, idPaint);
//    canvas.drawText(
//        "happiness: " + String.format("%.2f", face.getSmilingProbability()),
//        x + ID_X_OFFSET * 3,
//        y - ID_Y_OFFSET,
//        idPaint);
    if (facing == CameraSource.CAMERA_FACING_FRONT) {
      canvas.drawText(
          "right eye: " + String.format("%.2f", face.getRightEyeOpenProbability()),
          x - ID_X_OFFSET,
          y,
          idPaint);
      canvas.drawText(
          "left eye: " + String.format("%.2f", face.getLeftEyeOpenProbability()),
          x + ID_X_OFFSET * 6,
          y,
          idPaint);
    } else {
      canvas.drawText(
          "left eye: " + String.format("%.2f", face.getLeftEyeOpenProbability()),
          x - ID_X_OFFSET,
          y,
          idPaint);
      canvas.drawText(
          "right eye: " + String.format("%.2f", face.getRightEyeOpenProbability()),
          x + ID_X_OFFSET * 6,
          y,
          idPaint);
    }

    // Draws a bounding box around the face.
    float xOffset = scaleX(face.getBoundingBox().width() / 2.0f);
    float yOffset = scaleY(face.getBoundingBox().height() / 2.0f);
    float left1 = x - xOffset;
    float top = y - yOffset;
    float right1 = x + xOffset;
    float bottom = y + yOffset;
    canvas.drawRect(left1, top, right1, bottom, boxPaint);
  }
}
