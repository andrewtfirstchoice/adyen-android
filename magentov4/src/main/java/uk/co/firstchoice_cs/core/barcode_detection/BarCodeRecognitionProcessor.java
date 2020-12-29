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
package uk.co.firstchoice_cs.core.barcode_detection;

import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.co.firstchoice_cs.core.others.FrameMetadata;
import uk.co.firstchoice_cs.core.others.GraphicOverlay;


public class BarCodeRecognitionProcessor {

	private static final String TAG = "TextRecProc";
	private BarCodeRecognitionInterface callback;
	//private final FirebaseVisionTextRecognizer detector;
	private final FirebaseVisionBarcodeDetector detector;
	public Rect previewBoundingBox = new Rect(0,0,500,500);
	public String cachedBarCode = "";
	private boolean inPreviewWindow(FirebaseVisionBarcode barcode)
	{
		Rect cornerPoints =  barcode.getBoundingBox();
		if(previewBoundingBox==null||cornerPoints==null)
			return false;
		return cornerPoints.intersect(previewBoundingBox);
	}

	// Whether we should ignore process(). This is usually caused by feeding input data faster than
	// the model can handle.
	private final AtomicBoolean shouldThrottle = new AtomicBoolean(false);

	public BarCodeRecognitionProcessor(BarCodeRecognitionInterface callbackInterface) {
		detector = FirebaseVision.getInstance().getVisionBarcodeDetector();
		this.callback = callbackInterface;
	}



	//region ----- Exposed Methods -----


	public void stop() {
		try {
			detector.close();
		} catch (IOException e) {
			Log.e(TAG, "Exception thrown while trying to close Detector: " + e);
		}
	}


	public void process(ByteBuffer data, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay) throws FirebaseMLException {

		if (shouldThrottle.get()) {
			return;
		}
		FirebaseVisionImageMetadata metadata =
				new FirebaseVisionImageMetadata.Builder()
						.setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
						.setWidth(frameMetadata.getWidth())
						.setHeight(frameMetadata.getHeight())
						.setRotation(frameMetadata.getRotation())
						.build();

		detectInVisionImage(FirebaseVisionImage.fromByteBuffer(data, metadata), frameMetadata, graphicOverlay);


	}

	//endregion

	//region ----- Helper Methods -----

	protected Task<List<FirebaseVisionBarcode>> detectInImage(FirebaseVisionImage image) {
		return detector.detectInImage(image);
	}


	protected void onSuccess(@NonNull List<FirebaseVisionBarcode> results, @NonNull FrameMetadata frameMetadata, @NonNull GraphicOverlay graphicOverlay) {
		String regEx = "^[0-9]{7}$";
		graphicOverlay.clear();

		for (FirebaseVisionBarcode barcode : results) {
			boolean valid = false;
			boolean possiblePartNumber = false;
			boolean isQr = false;

			if (!inPreviewWindow(barcode)) {
				continue;
			}

			//do pattern check
			if (barcode.getFormat() == FirebaseVisionBarcode.FORMAT_QR_CODE) {
				isQr = true;
				Pattern mPattern = Pattern.compile(regEx);
				Matcher matcher = mPattern.matcher(barcode.getDisplayValue());
				if (matcher.find())
					valid = true;
				else
					possiblePartNumber = true;
			} else {
				valid = isValidSevenDigit(regEx, barcode, valid);
			}

			if (valid || possiblePartNumber) {
				GraphicOverlay.Graphic textGraphic = new BarcodeGraphic(graphicOverlay, barcode);
				graphicOverlay.add(textGraphic);

				if(cachedBarCode.equals(barcode.getDisplayValue()))
					return;
				else
					cachedBarCode = barcode.getDisplayValue();

				if (!callback.barCodeInList(barcode)) {
					callback.barcodeFound(barcode, possiblePartNumber,isQr);
				}
				else {
					int searchPos = callback.barCodeInCurrent(barcode);
					if(searchPos !=-1) {
						callback.moveCurrentListToIndex(searchPos);
						return;
					}
					searchPos = callback.barCodeInPrevious(barcode);
					if(searchPos!=-1)
						callback.movePreviousListToIndex(searchPos);
				}
			}
		}
	}

	private boolean isValidSevenDigit(String regEx, FirebaseVisionBarcode barcode, boolean valid) {
		Pattern mPattern = Pattern.compile(regEx);
		Matcher matcher = mPattern.matcher(barcode.getDisplayValue());
		if (matcher.find())
			valid = true;
		return valid;
	}



	protected void onFailure(@NonNull Exception e) {
		Log.w(TAG, "Text detection failed." + e);
	}

	private void detectInVisionImage(FirebaseVisionImage image, final FrameMetadata metadata, final GraphicOverlay graphicOverlay) {

		detectInImage(image)
				.addOnSuccessListener(firebaseVisionBarcodes -> {
					shouldThrottle.set(false);
					BarCodeRecognitionProcessor.this.onSuccess(firebaseVisionBarcodes, metadata, graphicOverlay);
				})

				.addOnFailureListener(
						e -> {
							shouldThrottle.set(false);
							BarCodeRecognitionProcessor.this.onFailure(e);
						});
		// Begin throttling until this frame of input has been processed, either in onAllDownloadsComplete or
		// onFailure.
		shouldThrottle.set(true);
	}

	//endregion


}
