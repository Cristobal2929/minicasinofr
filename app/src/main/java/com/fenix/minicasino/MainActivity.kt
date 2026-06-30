package com.fenix.minicasino

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.GradientDrawable
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var tvPuntos: TextView
    private lateinit var contenedor: FrameLayout
    private lateinit var prefs: SharedPreferences
    private var puntosUsuario: Int = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvPuntos = findViewById(R.id.tv_puntos)
        contenedor = findViewById(R.id.contenedor_juegos)

        prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        puntosUsuario = prefs.getInt("puntos", 100)
        actualizarPuntos()

        mostrarMenu()
    }

    private fun actualizarPuntos() {
        tvPuntos.text = "💰 Puntos: $puntosUsuario"
    }

    private fun guardarPuntos() {
        prefs.edit().putInt("puntos", puntosUsuario).apply()
    }

    private fun mostrarMenu() {
        contenedor.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        fun crearBoton(texto: String, id: Int, onClick: () -> Unit): Button {
            val btn = Button(this)
            btn.id = id
            btn.text = texto
            btn.setTextColor(Color.WHITE)
            btn.textSize = 18f
            btn.setAllCaps(false)
            btn.setTypeface(null, Typeface.BOLD)
            val drawable = GradientDrawable()
            drawable.setColor(Color.parseColor("#C41E3A"))
            drawable.cornerRadius = 16f
            btn.background = drawable
            btn.setOnClickListener { onClick() }
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 16, 0, 0)
            btn.layoutParams = params
            return btn
        }

        layout.addView(crearBoton("🎰 Ruleta", 1001) { iniciarRuleta() })
        layout.addView(crearBoton("🎟️ Rasca y Gana", 1002) { iniciarRasca() })
        layout.addView(crearBoton("🃏 Blackjack", 1003) { iniciarBlackjack() })
        layout.addView(crearBoton("🎱 Ruleta Casino", 1004) { iniciarRuletaCasino() })

        contenedor.addView(layout)
    }

    // ------------------- RUETA -------------------
    private fun iniciarRuleta() {
        contenedor.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A1633"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val ruletaView = RuletaView(this)
        layout.addView(
            ruletaView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val btnGirar = Button(this).apply {
            text = "Girar"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#C41E3A"))
        }
        layout.addView(
            btnGirar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 0) }
        )

        val btnVolver = crearBotonVolver()
        layout.addView(
            btnVolver,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 0) }
        )

        btnGirar.setOnClickListener {
            if (puntosUsuario < 10) {
                Toast.makeText(this, "Sin puntos suficientes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            puntosUsuario -= 10
            guardarPuntos()
            actualizarPuntos()
            btnGirar.isEnabled = false

            val inicio = ruletaView.rotacionActual
            val extra = Random.nextInt(0, 360)
            val fin = inicio + 1440 + extra // 4 full rotations + random
            val animator = ValueAnimator.ofFloat(inicio, fin.toFloat())
            animator.duration = 3000
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener {
                ruletaView.rotacionActual = it.animatedValue as Float
                ruletaView.invalidate()
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    val sector = ((ruletaView.rotacionActual % 360f) / 45f).toInt()
                    val multiplicadores = arrayOf(0, 0, 0, 2, 2, 3, 5, 10)
                    val ganancia = multiplicadores[sector] * 10
                    if (ganancia > 0) {
                        puntosUsuario += ganancia
                        Toast.makeText(
                            this@MainActivity,
                            "Ganaste $ganancia puntos!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "No ganaste puntos.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    guardarPuntos()
                    actualizarPuntos()
                    btnGirar.isEnabled = true
                }
            })
            animator.start()
        }

        btnVolver.setOnClickListener { mostrarMenu() }

        contenedor.addView(layout)
    }

    inner class RuletaView(context: Context) : View(context) {
        var rotacionActual = 0f
        private val paintSector = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        private val paintBorder = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = Color.parseColor("#D4AF37")
        }
        private val paintText = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        private val paintArrow = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#D4AF37")
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val size = Math.min(width, height).toFloat()
            val radius = size / 2 * 0.9f
            val cx = width / 2f
            val cy = height / 2f
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

            // Dibujar sectores girados
            canvas.save()
            canvas.rotate(rotacionActual, cx, cy)
            val colores = arrayOf(
                Color.parseColor("#C41E3A"),
                Color.parseColor("#1B2B4D")
            )
            for (i in 0 until 8) {
                paintSector.color = colores[i % 2]
                canvas.save()
                canvas.rotate(i * 45f, cx, cy)
                canvas.drawArc(rect, 0f, 45f, true, paintSector)
                canvas.restore()
            }
            canvas.restore()

            // Borde
            canvas.drawCircle(cx, cy, radius, paintBorder)

            // Texto multiplicadores
            val multiplicadores = arrayOf("x0", "x0", "x0", "x2", "x2", "x3", "x5", "x10")
            for (i in 0 until 8) {
                val angle = Math.toRadians((i * 45 + 22.5).toDouble())
                val tx = (cx + (radius * 0.6) * cos(angle)).toFloat()
                val ty = (cy + (radius * 0.6) * sin(angle)).toFloat() + 15f
                canvas.drawText(multiplicadores[i], tx, ty, paintText)
            }

            // Flecha arriba (no gira)
            val path = Path()
            path.moveTo(cx, cy - radius - 30)
            path.lineTo(cx - 30, cy - radius + 10)
            path.lineTo(cx + 30, cy - radius + 10)
            path.close()
            canvas.drawPath(path, paintArrow)
        }
    }

    // ------------------- RASCA Y GANA -------------------
    private fun iniciarRasca() {
        contenedor.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A1633"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val tvSimbolos = arrayOf(
            TextView(this),
            TextView(this),
            TextView(this)
        )
        tvSimbolos.forEach {
            it.textSize = 48f
            it.textAlignment = View.TEXT_ALIGNMENT_CENTER
            it.setTextColor(Color.WHITE)
            layout.addView(
                it,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 16, 0, 0) }
            )
        }

        val btnComprar = Button(this).apply {
            text = "Comprar carta"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#C41E3A"))
        }
        layout.addView(
            btnComprar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 24, 0, 0) }
        )

        val btnVolver = crearBotonVolver()
        layout.addView(
            btnVolver,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 0) }
        )

        val simbolos = arrayOf("🍒", "🍋", "🔔", "💎", "7️⃣")

        btnComprar.setOnClickListener {
            if (puntosUsuario < 5) {
                Toast.makeText(this, "Sin puntos suficientes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            puntosUsuario -= 5
            guardarPuntos()
            actualizarPuntos()

            val elegidos = List(3) { simbolos.random() }
            tvSimbolos.forEachIndexed { index, tv -> tv.text = elegidos[index] }

            val counts = elegidos.groupingBy { it }.eachCount()
            val maxCount = counts.values.maxOrNull() ?: 0
            val ganancia = when (maxCount) {
                3 -> 50
                2 -> 15
                else -> 0
            }
            if (ganancia > 0) {
                puntosUsuario += ganancia
                Toast.makeText(this, "Ganaste $ganancia puntos!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No ganaste puntos.", Toast.LENGTH_SHORT).show()
            }
            guardarPuntos()
            actualizarPuntos()
        }

        btnVolver.setOnClickListener { mostrarMenu() }

        contenedor.addView(layout)
    }

    // ------------------- BLACKJACK -------------------
    private fun iniciarBlackjack() {
        contenedor.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A1633"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val tvJugador = TextView(this).apply {
            text = "Jugador: "
            setTextColor(Color.WHITE)
            textSize = 18f
        }
        val tvDealer = TextView(this).apply {
            text = "Banca: "
            setTextColor(Color.WHITE)
            textSize = 18f
        }
        val tvResultado = TextView(this).apply {
            text = ""
            setTextColor(Color.YELLOW)
            textSize = 20f
        }

        layout.addView(tvJugador)
        layout.addView(tvDealer)
        layout.addView(tvResultado)

        val btnRepartir = Button(this).apply {
            text = "Repartir"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#C41E3A"))
        }
        val btnPedir = Button(this).apply {
            text = "Pedir carta"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#C41E3A"))
            isEnabled = false
        }
        val btnPlantarse = Button(this).apply {
            text = "Plantarse"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#C41E3A"))
            isEnabled = false
        }

        layout.addView(btnRepartir)
        layout.addView(btnPedir)
        layout.addView(btnPlantarse)

        val btnVolver = crearBotonVolver()
        layout.addView(
            btnVolver,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 0) }
        )

        var cartasJugador = mutableListOf<Int>()
        var cartasDealer = mutableListOf<Int>()
        val apuesta = 10

        fun actualizarPantalla() {
            tvJugador.text =
                "Jugador: ${cartasJugador.joinToString(", ")} (Total: ${cartasJugador.sum()})"
            val dealerVisible = if (cartasDealer.isNotEmpty()) cartasDealer[0].toString() else ""
            tvDealer.text = "Banca: $dealerVisible, ?"
        }

        btnRepartir.setOnClickListener {
            if (puntosUsuario < apuesta) {
                Toast.makeText(this, "Sin puntos suficientes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            puntosUsuario -= apuesta
            guardarPuntos()
            actualizarPuntos()

            cartasJugador.clear()
            cartasDealer.clear()
            cartasJugador.add(Random.nextInt(1, 12))
            cartasJugador.add(Random.nextInt(1, 12))
            cartasDealer.add(Random.nextInt(1, 12))
            cartasDealer.add(Random.nextInt(1, 12))
            actualizarPantalla()
            btnPedir.isEnabled = true
            btnPlantarse.isEnabled = true
            btnRepartir.isEnabled = false
        }

        btnPedir.setOnClickListener {
            cartasJugador.add(Random.nextInt(1, 12))
            actualizarPantalla()
            val total = cartasJugador.sum()
            if (total > 21) {
                tvResultado.text = "Te pasaste! Pierdes."
                btnPedir.isEnabled = false
                btnPlantarse.isEnabled = false
                btnRepartir.isEnabled = true
            }
        }

        btnPlantarse.setOnClickListener {
            // revelar dealer
            tvDealer.text =
                "Banca: ${cartasDealer.joinToString(", ")} (Total: ${cartasDealer.sum()})"
            var dealerTotal = cartasDealer.sum()
            while (dealerTotal < 17) {
                val nueva = Random.nextInt(1, 12)
                cartasDealer.add(nueva)
                dealerTotal = cartasDealer.sum()
            }
            tvDealer.text = "Banca: ${cartasDealer.joinToString(", ")} (Total: $dealerTotal)"
            val jugadorTotal = cartasJugador.sum()
            when {
                dealerTotal > 21 -> {
                    puntosUsuario += apuesta * 2
                    tvResultado.text = "Banca se pasa! Ganas."
                }
                dealerTotal == jugadorTotal -> {
                    puntosUsuario += apuesta
                    tvResultado.text = "Empate. Recuperas apuesta."
                }
                jugadorTotal > dealerTotal -> {
                    puntosUsuario += apuesta * 2
                    tvResultado.text = "Ganas."
                }
                else -> {
                    tvResultado.text = "Pierdes."
                }
            }
            guardarPuntos()
            actualizarPuntos()
            btnPedir.isEnabled = false
            btnPlantarse.isEnabled = false
            btnRepartir.isEnabled = true
        }

        btnVolver.setOnClickListener { mostrarMenu() }

        contenedor.addView(layout)
    }

    // ------------------- RUETA CASINO -------------------
    private fun iniciarRuletaCasino() {
        contenedor.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A1633"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val ruletaCasinoView = RuletaCasinoView(this)
        layout.addView(
            ruletaCasinoView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val btnRojo = Button(this).apply {
            text = "Rojo"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#C41E3A"))
        }
        val btnNegro = Button(this).apply {
            text = "Negro"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1A1A1A"))
        }
        val btnVerde = Button(this).apply {
            text = "Verde"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2E7D32"))
        }

        val btnGroup = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        btnGroup.addView(btnRojo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        btnGroup.addView(btnNegro, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        btnGroup.addView(btnVerde, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        layout.addView(
            btnGroup,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 0) }
        )

        val editNumero = EditText(this).apply {
            hint = "Número (0-11)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }
        layout.addView(
            editNumero,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 0) }
        )

        val btnGirar = Button(this).apply {
            text = "Girar"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#C41E3A"))
        }
        layout.addView(
            btnGirar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 0) }
        )

        val btnVolver = crearBotonVolver()
        layout.addView(
            btnVolver,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 0) }
        )

        var colorSeleccionado: String? = null
        fun actualizarSeleccion(btn: Button, color: String) {
            colorSeleccionado = color
            btnRojo.alpha = if (color == "rojo") 1f else 0.5f
            btnNegro.alpha = if (color == "negro") 1f else 0.5f
            btnVerde.alpha = if (color == "verde") 1f else 0.5f
        }

        btnRojo.setOnClickListener { actualizarSeleccion(btnRojo, "rojo") }
        btnNegro.setOnClickListener { actualizarSeleccion(btnNegro, "negro") }
        btnVerde.setOnClickListener { actualizarSeleccion(btnVerde, "verde") }

        btnGirar.setOnClickListener {
            if (puntosUsuario < 15) {
                Toast.makeText(this, "Sin puntos suficientes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            puntosUsuario -= 15
            guardarPuntos()
            actualizarPuntos()
            btnGirar.isEnabled = false

            val numeroIngresado = editNumero.text.toString().toIntOrNull()
            val numeroApostado = if (numeroIngresado != null && numeroIngresado in 0..11) numeroIngresado else null

            val inicio = ruletaCasinoView.anguloBola
            val extra = Random.nextInt(0, 360)
            val fin = inicio + extra
            val animator = ValueAnimator.ofFloat(inicio, fin.toFloat())
            animator.duration = 3500
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener {
                ruletaCasinoView.anguloBola = it.animatedValue as Float
                ruletaCasinoView.invalidate()
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    val sector = ((ruletaCasinoView.anguloBola % 360f) / 30f).toInt()
                    val colorGanador = when {
                        sector == 0 -> "verde"
                        sector % 2 == 1 -> "rojo"
                        else -> "negro"
                    }
                    var ganancia = 0
                    if (numeroApostado != null && numeroApostado == sector) {
                        ganancia = 150
                    } else if (colorSeleccionado != null && colorSeleccionado == colorGanador) {
                        ganancia = 30
                    }
                    if (ganancia > 0) {
                        puntosUsuario += ganancia
                        Toast.makeText(
                            this@MainActivity,
                            "Ganaste $ganancia puntos!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "No ganaste puntos.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    guardarPuntos()
                    actualizarPuntos()
                    btnGirar.isEnabled = true
                }
            })
            animator.start()
        }

        btnVolver.setOnClickListener { mostrarMenu() }

        contenedor.addView(layout)
    }

    inner class RuletaCasinoView(context: Context) : View(context) {
        var anguloBola = 0f
        private val paintSector = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        private val paintBorder = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = Color.parseColor("#D4AF37")
        }
        private val paintText = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        private val paintBall = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#D4AF37")
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val size = Math.min(width, height).toFloat()
            val radius = size / 2 * 0.9f
            val cx = width / 2f
            val cy = height / 2f
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

            // Dibujar sectores
            for (i in 0 until 12) {
                when {
                    i == 0 -> paintSector.color = Color.parseColor("#2E7D32") // verde
                    i % 2 == 1 -> paintSector.color = Color.parseColor("#C41E3A") // rojo
                    else -> paintSector.color = Color.parseColor("#1A1A1A") // negro
                }
                canvas.save()
                canvas.rotate(i * 30f, cx, cy)
                canvas.drawArc(rect, 0f, 30f, true, paintSector)
                canvas.restore()
            }

            // Borde
            canvas.drawCircle(cx, cy, radius, paintBorder)

            // Números
            for (i in 0 until 12) {
                val angle = Math.toRadians((i * 30 + 15).toDouble())
                val tx = (cx + (radius * 0.7) * cos(angle)).toFloat()
                val ty = (cy + (radius * 0.7) * sin(angle)).toFloat() + 10f
                canvas.drawText(i.toString(), tx, ty, paintText)
            }

            // Bola
            val ballRadius = radius * 0.07f
            val ballAngleRad = Math.toRadians(anguloBola.toDouble())
            val bx = (cx + (radius * 0.6) * cos(ballAngleRad)).toFloat()
            val by = (cy + (radius * 0.6) * sin(ballAngleRad)).toFloat()
            canvas.drawCircle(bx, by, ballRadius, paintBall)
        }
    }

    // ------------------- UTILIDADES -------------------
    private fun crearBotonVolver(): Button {
        val btn = Button(this)
        btn.text = "← Volver"
        btn.setTextColor(Color.parseColor("#D4AF37"))
        btn.setTypeface(null, Typeface.BOLD)
        val drawable = GradientDrawable()
        drawable.setColor(Color.TRANSPARENT)
        drawable.setStroke(4, Color.parseColor("#D4AF37"))
        drawable.cornerRadius = 16f
        btn.background = drawable
        return btn
    }
}