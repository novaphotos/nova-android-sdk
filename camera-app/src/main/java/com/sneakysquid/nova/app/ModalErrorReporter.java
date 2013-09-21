package com.sneakysquid.nova.app;

import static com.sneakysquid.nova.util.Debug.debug;

/**
 * @author Joe Walnes
 */
public class ModalErrorReporter implements ErrorReporter {

    @Override
    public void reportError(String msg) {
        // TODO
        debug("ERROR: %s", msg);
    }
}
