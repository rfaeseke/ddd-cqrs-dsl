package org.fuin.dsl.ddd.gen.extensions

/**
 * Turns model text into the inside of a Java string literal.
 *
 * <p>The parser hands a {@code STRING} over without its escapes - {@code message "Kernzeit \"von\" ..."} arrives as
 * {@code Kernzeit "von" ...} - so a template that writes it between two quotes as it stands ends the literal at the first
 * quote, and the generated class does not compile. Every template that puts model text (a message, a label, a tooltip, a
 * prompt, an example) into a literal goes through this.</p>
 */
class JavaStringExtensions {

    /**
     * Escapes backslash, quote and the line breaks. Null stays null, so an absent text keeps whatever the template does
     * with it.
     *
     * @param text Text as the model states it.
     *
     * @return Text that can stand between two double quotes in Java source.
     */
    static def String javaString(String text) {
        if (text === null) {
            return null
        }
        val sb = new StringBuilder(text.length + 8)
        for (var i = 0; i < text.length; i++) {
            val ch = text.charAt(i)
            switch (ch) {
                case ch == '\\'.charAt(0): sb.append("\\\\")
                case ch == '"'.charAt(0): sb.append("\\\"")
                case ch == '\n'.charAt(0): sb.append("\\n")
                case ch == '\r'.charAt(0): sb.append("\\r")
                case ch == '\t'.charAt(0): sb.append("\\t")
                default: sb.append(ch)
            }
        }
        return sb.toString
    }

}
