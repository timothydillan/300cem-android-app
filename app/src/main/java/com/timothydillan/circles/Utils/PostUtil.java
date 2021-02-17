package com.timothydillan.circles.Utils;

import android.content.Context;
import android.util.Log;

import com.timothydillan.circles.Models.Post;

import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier;

import java.io.IOException;
import java.util.List;

public class PostUtil {
    private static final String TAG = "PostUtil";
    private static final String ML_MODEL_PATH = "sentiment_analysis.tflite";

    private Context ctx;

    private NLClassifier postClassifier;

    public PostUtil(Context context) {
        ctx = context;
    }

    public void initializeModel() {
        try {
            postClassifier = NLClassifier.createFromFile(ctx, ML_MODEL_PATH);
            Log.d(TAG, "Successfully initialized sentiment analysis model.");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Failure in opening model file.");
        }
    }

    private String postCategory(Post post) {
        String textToAnalyze = post.getTitle();
        String sentiment = "Positive";

        if (!post.getDescription().isEmpty()) {
            textToAnalyze = textToAnalyze + " " + post.getDescription();
        }

        List<Category> classificationResults = postClassifier.classify(textToAnalyze);

        for (int i = 0; i < classificationResults.size(); i++) {
            Category result = classificationResults.get(i);
            if (result.getScore() >= 0.5) {
                sentiment = result.getLabel().equals("0") ? "Negative" : "Positive";
            }
        }

        return sentiment;
    }
}
