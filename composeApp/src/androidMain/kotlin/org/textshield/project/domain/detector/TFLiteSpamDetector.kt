package org.textshield.project.domain.detector

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier
import org.textshield.project.domain.model.DetectionMethod
import org.textshield.project.domain.model.SpamFilterResult
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Spam detector that uses TensorFlow Lite for text classification
 */
class TFLiteSpamDetector(private val context: Context) : SpamDetector {
    
    private var classifier: NLClassifier? = null
    private val modelName = "sms_spam_model.tflite"
    private val spamLabel = "spam"  // Label that the model uses for spam classification
    private val threshold = 0.5f    // Minimum confidence to classify as spam
    
    /**
     * Initialize the TF Lite text classifier
     */
    override suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            // In a real app, you'd have a pre-trained model in assets or download it.
            // For this example, we'll fall back to a rule-based approach if model isn't found
            val modelFile = File(context.cacheDir, modelName)
            
            // Check if we already have the model file
            if (!modelFile.exists()) {
                // In a real app, we'd download or extract the model
                // For this example, we'll just create an empty file to simulate model existence
                // This won't work for inference but demonstrates the pattern
                createDummyModelFile(modelFile)
            }
            
            // Create classifier from the model file path
            classifier = try {
                NLClassifier.createFromFile(modelFile)
            } catch (e: Exception) {
                // Log error and continue - we'll use rule-based fallback
                e.printStackTrace()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Failed to initialize - we'll use rule-based fallback
        }
    }
    
    override fun isInitialized(): Boolean {
        return classifier != null
    }
    
    /**
     * Use TF Lite to classify SMS message as spam or not
     */
    override suspend fun detectSpam(messageContent: String): SpamFilterResult = withContext(Dispatchers.Default) {
        try {
            // Fall back to rule-based detection if classifier not available
            if (classifier == null) {
                return@withContext fallbackDetector.detectSpam(messageContent)
            }
            
            // Run classification on the text
            val results = classifier!!.classify(messageContent)
            
            // Search for spam prediction among results
            var isSpam = false
            var confidenceScore = 0.0f
            
            for (category: Category in results) {
                // Check if this is the spam label
                if (category.label.equals(spamLabel, ignoreCase = true) && 
                    category.score >= threshold) {
                    isSpam = true
                    confidenceScore = category.score
                    break
                }
            }
            
            SpamFilterResult(
                isSpam = isSpam,
                confidenceScore = confidenceScore,
                detectionMethod = DetectionMethod.MACHINE_LEARNING
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fall back to rule-based on any error
            fallbackDetector.detectSpam(messageContent)
        }
    }
    
    // Use rule-based detector as fallback
    private val fallbackDetector = RuleBasedSpamDetector().apply {
        // Initialize the fallback detector
        kotlinx.coroutines.runBlocking { initialize() }
    }
    
    /**
     * Create an empty model file for demonstration purposes
     * In a real app, you'd download or extract a real model
     */
    private fun createDummyModelFile(modelFile: File) {
        try {
            FileOutputStream(modelFile).use { fos ->
                // Write some bytes to create the file
                fos.write(byteArrayOf(0, 0, 0, 0))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
} 