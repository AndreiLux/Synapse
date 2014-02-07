/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.utils;

import com.af.synapse.elements.BaseElement;

/**
 * Created by Andrei on 06/02/14.
 */
public class ElementFailureException extends Exception {
    private BaseElement source = null;
    private String sourceClassName = null;

    public ElementFailureException (String className, Exception exception) {
        super(exception);
        this.sourceClassName = className;
    }

    public ElementFailureException (BaseElement source, Exception exception) {
        super(exception);
        this.source = source;
        this.sourceClassName = source.getClass().getSimpleName();
    }

    public BaseElement getSource() {
        return source;
    }

    public String getSourceClass() {
        return sourceClassName;
    }
}
