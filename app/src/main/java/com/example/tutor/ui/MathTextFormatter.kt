package com.example.tutor.ui

object MathTextFormatter {

    /**
     * Convierte texto que contenga expresiones LaTeX o códigos matemáticos en texto legible y natural para seres humanos.
     */
    fun formatReadableMath(raw: String): String {
        if (raw.isBlank()) return raw

        var text = raw

        // Quitar delimitadores de LaTeX como $$ ... $$, \( ... \), \[ ... \] o backticks de fórmulas
        text = text.replace("$$", "")
        text = text.replace("\\[", "")
        text = text.replace("\\]", "")
        text = text.replace("\\(", "")
        text = text.replace("\\)", "")

        // Reemplazar fracciones LaTeX \frac{numerador}{denominador} -> (numerador / denominador)
        val fracRegex = "\\\\frac\\{([^{}]+)\\}\\{([^{}]+)\\}".toRegex()
        while (fracRegex.containsMatchIn(text)) {
            text = text.replace(fracRegex) { match ->
                val num = match.groupValues[1].trim()
                val den = match.groupValues[2].trim()
                "($num / $den)"
            }
        }

        // Reemplazar raíces cuadradas \sqrt{x} o \sqrt[n]{x} -> √(x)
        val sqrtRegex = "\\\\sqrt(?:\\[([^{}]+)\\])?\\{([^{}]+)\\}".toRegex()
        text = text.replace(sqrtRegex) { match ->
            val rootDegree = match.groupValues[1]
            val content = match.groupValues[2].trim()
            if (rootDegree.isNotBlank()) "${rootDegree}√($content)" else "√($content)"
        }

        // Reemplazos de comandos matemáticos comunes en LaTeX a símbolos Unicode amigables
        val replacements = mapOf(
            "\\pm" to "±",
            "\\times" to " × ",
            "\\cdot" to " · ",
            "\\div" to " ÷ ",
            "\\leq" to " ≤ ",
            "\\le" to " ≤ ",
            "\\geq" to " ≥ ",
            "\\ge" to " ≥ ",
            "\\neq" to " ≠ ",
            "\\ne" to " ≠ ",
            "\\approx" to " ≈ ",
            "\\infty" to "∞",
            "\\pi" to "π",
            "\\theta" to "θ",
            "\\alpha" to "α",
            "\\beta" to "β",
            "\\gamma" to "γ",
            "\\delta" to "δ",
            "\\Delta" to "Δ",
            "\\sum" to "∑",
            "\\int" to "∫",
            "\\to" to " → ",
            "\\rightarrow" to " → ",
            "\\leftarrow" to " ← ",
            "\\left(" to "(",
            "\\right)" to ")",
            "\\left[" to "[",
            "\\right]" to "]",
            "\\left\\{" to "{",
            "\\right\\}" to "}",
            "\\{" to "{",
            "\\}" to "}",
            "\\quad" to " ",
            "\\qquad" to "  ",
            "\\text{" to "",
            "\\mathbf{" to "",
            "\\mathit{" to "",
            "\\mathrm{" to ""
        )

        for ((latex, symbol) in replacements) {
            text = text.replace(latex, symbol)
        }

        // Convertir potencias simples como x^2 -> x², x^3 -> x³, x^0 -> x⁰, etc.
        val powerMap = mapOf(
            "^0" to "⁰", "^1" to "¹", "^2" to "²", "^3" to "³", "^4" to "⁴",
            "^5" to "⁵", "^6" to "⁶", "^7" to "⁷", "^8" to "⁸", "^9" to "⁹",
            "^{0}" to "⁰", "^{1}" to "¹", "^{2}" to "²", "^{3}" to "³", "^{4}" to "⁴",
            "^{5}" to "⁵", "^{6}" to "⁶", "^{7}" to "⁷", "^{8}" to "⁸", "^{9}" to "⁹",
            "^{n}" to "ⁿ", "^{x}" to "ˣ", "^{y}" to "ʸ", "^{a}" to "ᵃ", "^{b}" to "ᵇ"
        )
        for ((exp, unicode) in powerMap) {
            text = text.replace(exp, unicode)
        }

        // Convertir subíndices simples como x_1 -> x₁, x_2 -> x₂, x_0 -> x₀
        val subMap = mapOf(
            "_0" to "₀", "_1" to "₁", "_2" to "₂", "_3" to "₃", "_4" to "₄",
            "_5" to "₅", "_6" to "₆", "_7" to "₇", "_8" to "₈", "_9" to "₉",
            "_{0}" to "₀", "_{1}" to "₁", "_{2}" to "₂", "_{3}" to "₃", "_{4}" to "₄",
            "_{5}" to "₅", "_{6}" to "₆", "_{7}" to "₇", "_{8}" to "₈", "_{9}" to "₉",
            "_{n}" to "ₙ", "_{i}" to "ᵢ"
        )
        for ((sub, unicode) in subMap) {
            text = text.replace(sub, unicode)
        }

        // Limpiar llaves residuales de comandos cerrados
        text = text.replace("\\", "")

        return text
    }
}
