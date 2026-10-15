/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package p.shared.domain.x.aggregateid;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serial;
import javax.annotation.concurrent.Immutable;
import org.fuin.ddd4j.core.HasEntityTypeConstant;
import org.fuin.objects4j.common.HasPublicStaticIsValidMethod;
import org.fuin.objects4j.common.HasPublicStaticValueOfMethod;
import org.jspecify.annotations.Nullable;

/**
 * Aggregate ID multiple attributes and base.
 */
@Immutable
@HasEntityTypeConstant
@HasPublicStaticIsValidMethod
@HasPublicStaticValueOfMethod
@XmlJavaTypeAdapter(MyAggregate3IdConverter.class)
public final class MyAggregate3Id extends AbstractMyAggregate3Id {

    @Serial
    private static final long serialVersionUID = 1000L;
    
    /**
     * Default constructor.
     */
    @SuppressWarnings("NullAway.Init")
    protected MyAggregate3Id() {
        super();
    }
    
    /**
     * Constructor with all data.
     *
     * @param a Persistent value A.
     * @param b Persistent value B.
     */
    public MyAggregate3Id(final String a, final String b) {
        super(a, b);
    }
    
    @Override
    public final String asBaseType() {
        // TODO Implement!
        return null;
    }
    
    /**
     * Returns the information if a given string can be converted into
     * an instance of MyAggregate3Id. A <code>null</code> value returns <code>true</code>.
     * 
     * @param value
     *            Value to check.
     * 
     * @return TRUE if it's a valid string, else FALSE.
     */
    public static boolean isValid(@Nullable final String value) {
        if (value == null) {
            return true;
        }
        // TODO Verify the value is valid!
        return true;
    }
    
    /**
     * Parses a given string and returns a new instance of MyAggregate3Id.
     * 
     * @param value
     *            Value to convert. A <code>null</code> value returns
     *            <code>null</code>.
     * 
     * @return Converted value.
     */
    @Nullable
    public static MyAggregate3Id valueOf(@Nullable final String value) {
        if (value == null) {
            return null;
        }
        // TODO Parse string value and return new instance! 
        // return new MyAggregate3Id(value);
        return null;
    }
    
}
