package io.github.mac_genius.alexatransloc.translocspeechlet;

import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Mac Watson on 2/5/2017.
 */
public class TranslocSpeechletRequestStreamHandler extends SpeechletRequestStreamHandler {

    private static final Set<String> supportedApplicationIds;

    static {
        supportedApplicationIds = new HashSet<String>();
    }

    public TranslocSpeechletRequestStreamHandler() {
        super(new TranslocSpeechlet(), supportedApplicationIds);
    }

    public TranslocSpeechletRequestStreamHandler(Speechlet speechlet, Set<String> supportedApplicationIds) {
        super(speechlet, supportedApplicationIds);
    }
}
