package com.aqpseller.lulaapp.domain.legal

/**
 * Borradores de los 3 documentos legales que exige `Plan/11-cuentas-y-conexiones.md` (paso 4).
 * Son un punto de partida razonable, no el texto final — antes de publicar en Play Store hace
 * falta que alguien con conocimiento legal real (sobre todo pensando en la Ley N° 29733, Ley
 * de Protección de Datos Personales del Perú) los revise. Los datos entre `[corchetes]` son
 * placeholders que hay que completar con la identidad legal real antes de publicar.
 */
enum class TipoDocumentoLegal(val id: String, val titulo: String) {
    PRIVACIDAD("privacidad", "Política de Privacidad"),
    TERMINOS("terminos", "Términos de Servicio"),
    DATOS_SALUD("salud", "Datos de salud"),
}

object TextosLegales {

    fun textoPara(tipo: TipoDocumentoLegal): String = when (tipo) {
        TipoDocumentoLegal.PRIVACIDAD -> POLITICA_PRIVACIDAD
        TipoDocumentoLegal.TERMINOS -> TERMINOS_SERVICIO
        TipoDocumentoLegal.DATOS_SALUD -> CONSENTIMIENTO_DATOS_SALUD
    }

    private val POLITICA_PRIVACIDAD = """
        Última actualización: [fecha de publicación]

        Esta Política de Privacidad explica qué datos recopila Lula, cómo los usa y qué
        derechos tienes sobre ellos. Aplica a la aplicación móvil Lula ("la app"), operada por
        [nombre legal del responsable] ("nosotros").

        1. Qué datos recopilamos

        - Datos de cuenta: tu nombre, y tu correo si eliges iniciar sesión con uno.
        - Contenido que tú creas: hábitos, tareas, rutinas, movimientos financieros, entradas de
          diario, notas, respuestas de "Mi propósito", metas, y — si eliges usarlas —
          medicamentos y citas médicas.
        - Si usas un Espacio Familia: el contenido que decidas compartir ahí es visible para las
          personas que invites a ese espacio.
        - No recopilamos ubicación, contactos, ni acceso a cámara/galería salvo que una función
          futura lo pida explícitamente y tú la actives.

        2. Dónde viven tus datos

        Hoy Lula es una app local-first: todo lo que creas se guarda en tu propio dispositivo, no
        en un servidor nuestro. Si en el futuro activas una cuenta con sincronización, tus datos
        también se guardarán en nuestros servidores para poder mostrártelos en más de un
        dispositivo — te avisaremos explícitamente cuando esa función exista.

        3. Con quién compartimos tus datos

        No vendemos ni compartimos tus datos con terceros para publicidad. Las únicas excepciones:

        - Las personas que tú invitas a un Espacio Familia ven lo que decidas compartir ahí.
        - Si usas una función que llama a un servicio de inteligencia artificial (por ejemplo,
          "Armar y presentar con IA" en Mi propósito), el texto que escribiste se envía a ese
          proveedor de IA para procesarlo. Te lo señalamos en el momento, no ocurre en segundo
          plano.
        - Si algún día conectamos un flujo de automatización (por ejemplo n8n) para alguna
          función, lo indicaremos explícitamente antes de que empiece a usarse.

        4. Datos de salud (medicamentos y citas)

        La ley peruana trata la información de salud como una categoría sensible. Por eso te
        pedimos un consentimiento aparte, específico, antes de usar Medicamentos o Citas — ver
        el documento "Datos de salud". Nunca usamos esta información para publicidad ni la
        compartimos salvo en los casos ya descritos arriba.

        5. Cuánto tiempo guardamos tus datos

        Mientras tengas la cuenta activa. Puedes borrar cualquier elemento individual cuando
        quieras, o eliminar tu cuenta completa desde "Mi perfil → Eliminar mi cuenta", lo que
        borra todo de forma permanente e inmediata.

        6. Tus derechos

        Bajo la Ley N° 29733 tienes derecho a acceder, rectificar, cancelar y oponerte al
        tratamiento de tus datos personales (derechos ARCO). Puedes ejercer la mayoría de estos
        derechos directamente desde la app (editar o borrar cualquier dato); para lo demás,
        escríbenos a [correo de contacto].

        7. Menores de edad

        Lula pide confirmar que tienes al menos 13 años antes de usar la app.

        8. Cambios a esta política

        Si hacemos cambios importantes, te avisaremos dentro de la app antes de que entren en
        vigencia.

        9. Contacto

        [correo de contacto] — [nombre legal del responsable], [país].
    """.trimIndent()

    private val TERMINOS_SERVICIO = """
        Última actualización: [fecha de publicación]

        Estos Términos de Servicio son el acuerdo entre tú y [nombre legal del responsable]
        ("nosotros") para el uso de Lula ("la app"). Al usar la app aceptas estos términos.

        1. Qué es Lula

        Lula es un asistente personal para organizar hábitos, tareas, finanzas, medicamentos,
        citas, un diario y reflexiones personales ("Mi propósito"), pensado con un enfoque
        anti-punitivo: ningún intento cuenta como fallo.

        Lula no es un servicio médico ni de asesoría financiera profesional. Los recordatorios
        de medicamentos y citas son una ayuda de organización, no reemplazan la indicación de un
        profesional de salud; los registros financieros son para tu propio seguimiento, no
        constituyen asesoría de inversión.

        2. Tu cuenta

        Necesitas confirmar que tienes al menos 13 años para usar Lula. Sos responsable de
        mantener la seguridad de tu dispositivo y, si activás una cuenta con sincronización, de
        tus credenciales de acceso.

        3. Tu contenido

        Todo lo que creás en Lula (hábitos, notas, entradas de diario, respuestas de Mi
        propósito, etc.) es tuyo. Lula no reclama ningún derecho de propiedad sobre tu
        contenido; lo usamos únicamente para brindarte el servicio (mostrártelo, calcular tu
        progreso, etc.), y en los casos ya descritos en la Política de Privacidad cuando vos
        mismo activás una función que lo requiere (por ejemplo, pedirle a una IA que redacte tu
        Misión y Visión).

        4. Planes gratuito y de pago

        Lula tiene un plan gratuito con las funciones principales sin límite de uso. Algunas
        funciones que dependen de inteligencia artificial (como "Armar y presentar con IA")
        tienen un número limitado de usos gratis y después requieren un plan Premium, porque
        cada uso tiene un costo real para nosotros. Los detalles de cada plan se muestran dentro
        de la app antes de que tengas que pagar nada.

        5. Uso aceptable

        No está permitido usar Lula para actividades ilegales, para intentar acceder a datos de
        otras personas sin su consentimiento, ni para interferir con el funcionamiento de la
        app.

        6. Disponibilidad del servicio

        Lula está en desarrollo activo. Hacemos lo posible por mantener la app funcionando de
        forma confiable, pero no garantizamos que esté disponible sin interrupciones en todo
        momento.

        7. Cancelación

        Podés dejar de usar Lula cuando quieras y eliminar tu cuenta desde "Mi perfil →
        Eliminar mi cuenta", lo que borra todos tus datos de forma permanente.

        8. Límite de responsabilidad

        En la medida permitida por la ley, no somos responsables por decisiones que tomes
        basándote en la información de la app, incluyendo (sin limitarse a) recordatorios de
        medicamentos, citas, o registros financieros — son herramientas de apoyo, la decisión y
        la verificación final siempre son tuyas.

        9. Cambios a estos términos

        Si hacemos cambios importantes, te avisaremos dentro de la app antes de que entren en
        vigencia.

        10. Ley aplicable

        Estos términos se rigen por las leyes de la República del Perú.

        11. Contacto

        [correo de contacto] — [nombre legal del responsable], [país].
    """.trimIndent()

    private val CONSENTIMIENTO_DATOS_SALUD = """
        Medicamentos y citas médicas son datos de salud — una categoría de información que la
        Ley N° 29733 (Ley de Protección de Datos Personales del Perú) trata como sensible. Por
        eso te pedimos este consentimiento aparte, separado de la Política de Privacidad
        general.

        Qué implica aceptar:
        - Vas a poder crear Medicamentos y Citas, con sus horarios y recordatorios.
        - Esa información se guarda igual que el resto de tu contenido en Lula — hoy, local en
          tu dispositivo; si en el futuro activás sincronización, también en nuestros
          servidores, protegida igual que cualquier otro dato tuyo.
        - Nunca usamos esta información para publicidad ni la compartimos con nadie, salvo con
          quien tú decidas invitar a verla (por ejemplo, dentro de Mi círculo de cuidado).
        - Podés borrar un medicamento o cita individual en cualquier momento, o retirar este
          consentimiento eliminando tu cuenta completa desde "Mi perfil → Eliminar mi cuenta".

        Esto no reemplaza la indicación de ningún profesional de salud — los recordatorios son
        una ayuda de organización, no una receta ni un consejo médico.
    """.trimIndent()
}
