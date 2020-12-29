package uk.co.firstchoice_cs.core.barcode_detection;

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

/**
 * Processor for the text recognition demo.
 */

public interface BarCodeRecognitionInterface{
	void barcodeFound(FirebaseVisionBarcode barcode, boolean possiblePartNumber, boolean isQr);

	boolean barCodeInList(FirebaseVisionBarcode barcode);

	void movePreviousListToIndex(int searchPos);

	void moveCurrentListToIndex(int searchPos);

	int barCodeInPrevious(FirebaseVisionBarcode barcode);

	int barCodeInCurrent(FirebaseVisionBarcode barcode);
}
