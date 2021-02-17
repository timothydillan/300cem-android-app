package com.timothydillan.circles;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.timothydillan.circles.Services.WearableService;
import com.timothydillan.circles.Utils.HealthUtil;
import com.timothydillan.circles.Utils.PermissionUtil;

import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MoodsFragment extends Fragment {

    private static final String TAG = "MoodsFragment";
    private static final String MODEL_PATH = "sentiment_analysis.tflite";
    private ExecutorService executorService;
    private TextView resultTextView;
    private EditText inputEditText;
    private ScrollView scrollView;
    private Button predictButton;
    private NLClassifier textClassifier;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executorService = Executors.newSingleThreadExecutor();
        initializeModel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_moods, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        resultTextView = view.findViewById(R.id.result_text_view);
        inputEditText = view.findViewById(R.id.input_text);
        scrollView = view.findViewById(R.id.scroll_view);

        predictButton = view.findViewById(R.id.predict_button);
        predictButton.setOnClickListener(
                (View v) -> {
                    classify(inputEditText.getText().toString());
                });
    }

    private void initializeModel() {
        try {
            textClassifier = NLClassifier.createFromFile(requireContext(), MODEL_PATH);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Failure in opening model file.");
        }
    }

    /** Send input text to TextClassificationClient and get the classify messages. */
    private void classify(final String text) {
        executorService.execute(
            () -> {
                List<Category> results = textClassifier.classify(text);

                StringBuilder textToShow = new StringBuilder("Input: " + text + "\nOutput:\n");
                for (int i = 0; i < results.size(); i++) {
                    Category result = results.get(i);
                    textToShow.append(String.format("    %s: %s\n", result.getLabel(),
                            result.getScore()));
                }

                textToShow.append("---------\n");

                // Show classification result on screen
                showResult(textToShow.toString());
            });
    }

    /** Show classification result on the screen. */
    private void showResult(final String textToShow) {
        // Run on UI thread as we'll updating our app UI
        requireActivity().runOnUiThread(
                () -> {
                    // Append the result to the UI.
                    resultTextView.append(textToShow);

                    // Clear the input text.
                    inputEditText.getText().clear();

                    // Scroll to the bottom to show latest entry's classification result.
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                });
    }
}