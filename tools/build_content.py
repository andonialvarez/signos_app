"""Builds app/src/main/assets/content.json for Signos Daily.

Usage: python3 tools/build_content.py app/src/main/assets/content.json

Words: (word, category, tip, dilse_queries or None, key or None, label or None)
Expressions: (spanish, category, [sign keys or '#free text'], note)
"""
import json
import sys
import unicodedata


def slug(s):
    s = unicodedata.normalize("NFD", s.lower())
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    out = []
    for c in s:
        if c.isalnum():
            out.append(c)
        elif c in " -_":
            out.append("_")
    return "".join(out).strip("_")


EMO = "En LSE la cara cuenta tanto como la mano: pon la expresión que corresponde."
QW = "Al preguntar con qué, dónde, cuándo… se frunce el ceño: la cara marca la pregunta."

W = [
    # word, category, tip, queries, key, label
    ("hola", "Saludos", "Acompáñalo de una sonrisa: en LSE la cara también saluda.", None, None, None),
    ("casa", "La casa", "Recuerda el tejado de una casa: las manos se juntan por arriba.", None, None, None),
    ("yo", "Personas", "Los pronombres se signan señalando: para «yo», te señalas a ti.", None, None, None),
    ("tú", "Personas", "Para «tú», señalas a la persona con la que hablas.", ["tú", "tu"], "tu", None),
    ("nombre", "Presentarse", "Tu nombre se deletrea con el alfabeto dactilológico (lo tienes en Biblioteca > Abecedario). Muchas personas sordas tienen además un signo-nombre que les pone su comunidad.", None, None, None),
    ("gracias", "Cortesía", "", None, None, None),
    ("coche", "Transporte", "Recuerda el gesto de sujetar un volante.", None, None, None),
    ("bien", "Básicos", "", None, None, None),
    ("adiós", "Saludos", "", None, None, None),
    ("ducharse", "Rutina", "Recuerda la ducha: el agua que cae sobre la cabeza.", ["duchar", "ducha", "ducharse"], None, "DUCHARSE"),
    ("por favor", "Cortesía", "", None, None, None),
    ("sí", "Básicos", "Puedes acompañarlo asintiendo con la cabeza.", ["sí", "si"], "si", None),
    ("no", "Básicos", "Puedes acompañarlo negando con la cabeza.", None, None, None),
    ("sordo", "Personas", "«Sordo» o «persona sorda» es lo correcto y respetuoso. Evita «sordomudo»: las personas sordas tienen voz y lengua propia.", ["sordo"], None, None),
    ("oyente", "Personas", "Así se llama en la comunidad sorda a las personas que oyen.", None, None, None),
    ("signar", "Comunicación", "En lengua de signos no se «gesticula»: se signa.", ["signar", "lengua de signos", "signo"], None, None),
    ("entender", "Comunicación", "", ["entender", "comprender"], None, None),
    ("comer", "Comida", "Recuerda el gesto de llevarse comida a la boca.", None, None, None),
    ("beber", "Comida", "Recuerda el gesto de beber de un vaso.", None, None, None),
    ("agua", "Comida", "", None, None, None),
    ("amigo", "Personas", "", None, None, None),
    ("familia", "Familia", "", None, None, None),
    ("madre", "Familia", "", ["madre", "mamá"], None, None),
    ("padre", "Familia", "", ["padre", "papá"], None, None),
    ("hermano", "Familia", "", None, None, None),
    ("trabajar", "Trabajo y estudio", "", ["trabajar", "trabajo"], None, None),
    ("dormir", "Rutina", "", None, None, None),
    ("despertarse", "Rutina", "", ["despertar", "despertarse"], None, None),
    ("desayunar", "Comida", "", ["desayunar", "desayuno"], None, None),
    ("teléfono", "Objetos", "Recuerda el gesto de hablar por teléfono, con la mano junto a la oreja.", None, None, None),
    ("hoy", "Tiempo", "En LSE las palabras de tiempo (hoy, ayer, mañana) suelen ir al principio de la frase.", None, None, None),
    ("mañana", "Tiempo", "Cuidado: «mañana» (el día siguiente) y «por la mañana» pueden tener signos distintos.", None, None, None),
    ("ayer", "Tiempo", "", None, None, None),
    ("ahora", "Tiempo", "", None, None, None),
    ("después", "Tiempo", "", None, None, None),
    ("día", "Tiempo", "", None, None, None),
    ("noche", "Tiempo", "", None, None, None),
    ("semana", "Tiempo", "", None, None, None),
    ("libro", "Ocio", "Recuerda un libro que se abre.", None, None, None),
    ("escribir", "Comunicación", "Recuerda el gesto de escribir con un lápiz.", None, None, None),
    ("leer", "Comunicación", "", None, None, None),
    ("hablar", "Comunicación", "Es «hablar» de conversar con alguien. Hablar con la voz y signar tienen sus propios signos.", None, None, None),
    ("mirar", "Verbos", "La mirada sale de los ojos: fíjate en dónde empieza el signo.", ["mirar", "ver"], None, None),
    ("escuchar", "Verbos", "Fíjate en que se signa junto a la oreja.", ["escuchar", "oír"], None, None),
    ("querer", "Verbos", "Es «querer» de desear algo. Para «te quiero» hay otro signo.", None, None, None),
    ("gustar", "Verbos", "", None, None, None),
    ("saber", "Verbos", "", None, None, None),
    ("poder", "Verbos", "", None, None, None),
    ("ir", "Verbos", "", None, None, None),
    ("venir", "Verbos", "", None, None, None),
    ("hacer", "Verbos", "", None, None, None),
    ("tener", "Verbos", "", None, None, None),
    ("vivir", "Verbos", "", None, None, None),
    ("estudiar", "Trabajo y estudio", "", None, None, None),
    ("aprender", "Trabajo y estudio", "", None, None, None),
    ("colegio", "Lugares", "", ["colegio", "escuela"], None, None),
    ("profesor", "Personas", "", ["profesor", "maestro"], None, None),
    ("médico", "Salud", "Recuerda al médico tomando el pulso en la muñeca.", None, None, None),
    ("hospital", "Lugares", "", None, None, None),
    ("bicicleta", "Transporte", "Recuerda el movimiento de los pedales.", None, None, None),
    ("autobús", "Transporte", "", None, None, None),
    ("tren", "Transporte", "", None, None, None),
    ("avión", "Transporte", "Recuerda un avión que despega.", None, None, None),
    ("moto", "Transporte", "Recuerda el gesto de acelerar con el puño en el manillar.", ["moto", "motocicleta"], None, None),
    ("conducir", "Transporte", "Recuerda el gesto de girar el volante.", None, None, None),
    ("calle", "Lugares", "", None, None, None),
    ("ciudad", "Lugares", "", None, None, None),
    ("pueblo", "Lugares", "", None, None, None),
    ("tienda", "Lugares", "", None, None, None),
    ("comprar", "Compras", "", None, None, None),
    ("dinero", "Compras", "", None, None, None),
    ("caro", "Compras", EMO, None, None, None),
    ("barato", "Compras", "", None, None, None),
    ("pan", "Comida", "", None, None, None),
    ("leche", "Comida", "", None, None, None),
    ("café", "Comida", "", None, None, None),
    ("fruta", "Comida", "", None, None, None),
    ("manzana", "Comida", "", None, None, None),
    ("carne", "Comida", "", None, None, None),
    ("pescado", "Comida", "", None, None, None),
    ("huevo", "Comida", "", None, None, None),
    ("cocinar", "La casa", "", None, None, None),
    ("cocina", "La casa", "", None, None, None),
    ("baño", "La casa", "", ["baño", "váter"], None, None),
    ("dormitorio", "La casa", "", ["dormitorio", "habitación"], None, None),
    ("cama", "La casa", "", None, None, None),
    ("mesa", "La casa", "", None, None, None),
    ("silla", "La casa", "", None, None, None),
    ("puerta", "La casa", "", None, None, None),
    ("ventana", "La casa", "", None, None, None),
    ("llave", "La casa", "Recuerda el gesto de girar una llave en la cerradura.", None, None, None),
    ("lavarse las manos", "Rutina", "", ["lavarse las manos", "lavar"], "lavar", "LAVARSE-LAS-MANOS"),
    ("peinarse", "Rutina", "Recuerda el gesto de pasarse el peine.", ["peinar", "peine", "peinarse"], None, "PEINARSE"),
    ("vestirse", "Rutina", "", ["vestir", "vestirse"], None, "VESTIRSE"),
    ("ropa", "Ropa", "", None, None, None),
    ("zapato", "Ropa", "", None, None, None),
    ("pantalón", "Ropa", "", None, None, None),
    ("abrigo", "Ropa", "", None, None, None),
    ("frío", "Tiempo atmosférico", "Recuerda cómo se tiembla de frío.", None, None, None),
    ("calor", "Tiempo atmosférico", "", None, None, None),
    ("llover", "Tiempo atmosférico", "Recuerda las gotas de lluvia que caen.", ["llover", "lluvia"], None, None),
    ("sol", "Tiempo atmosférico", "", None, None, None),
    ("nieve", "Tiempo atmosférico", "Recuerda los copos que caen despacio.", ["nieve", "nevar"], None, None),
    ("viento", "Tiempo atmosférico", "", None, None, None),
    ("árbol", "Naturaleza", "Recuerda un árbol: el tronco y las ramas.", None, None, None),
    ("flor", "Naturaleza", "", None, None, None),
    ("mar", "Naturaleza", "", None, None, None),
    ("playa", "Naturaleza", "", None, None, None),
    ("montaña", "Naturaleza", "", None, None, None),
    ("perro", "Animales", "", None, None, None),
    ("gato", "Animales", "Recuerda los bigotes del gato.", None, None, None),
    ("pájaro", "Animales", "Recuerda el pico de un pájaro.", None, None, None),
    ("caballo", "Animales", "", None, None, None),
    ("rojo", "Colores", "", None, None, None),
    ("azul", "Colores", "", None, None, None),
    ("verde", "Colores", "", None, None, None),
    ("amarillo", "Colores", "", None, None, None),
    ("negro", "Colores", "", None, None, None),
    ("blanco", "Colores", "", None, None, None),
    ("color", "Colores", "", None, None, None),
    ("feliz", "Emociones", EMO, ["feliz", "contento", "alegre"], None, None),
    ("triste", "Emociones", EMO, None, None, None),
    ("cansado", "Emociones", EMO, None, None, None),
    ("enfadado", "Emociones", EMO, ["enfadado", "enfadar", "enfado"], None, None),
    ("nervioso", "Emociones", EMO, None, None, None),
    ("miedo", "Emociones", EMO, None, None, None),
    ("amor", "Emociones", "", None, None, None),
    ("bonito", "Adjetivos", "", ["bonito", "guapo"], None, None),
    ("grande", "Adjetivos", "", None, None, None),
    ("pequeño", "Adjetivos", "", None, None, None),
    ("nuevo", "Adjetivos", "", None, None, None),
    ("viejo", "Adjetivos", "", None, None, None),
    ("fácil", "Adjetivos", "", None, None, None),
    ("difícil", "Adjetivos", "", None, None, None),
    ("mucho", "Básicos", "", None, None, None),
    ("poco", "Básicos", "", None, None, None),
    ("más", "Básicos", "", None, None, None),
    ("qué", "Preguntas", QW, ["qué", "que"], None, "¿QUÉ?"),
    ("quién", "Preguntas", QW, ["quién", "quien"], None, "¿QUIÉN?"),
    ("dónde", "Preguntas", QW, ["dónde", "donde"], None, "¿DÓNDE?"),
    ("cuándo", "Preguntas", QW, ["cuándo", "cuando"], None, "¿CUÁNDO?"),
    ("cómo", "Preguntas", QW, ["cómo", "como"], None, "¿CÓMO?"),
    ("por qué", "Preguntas", "DILSE recoge este signo como «porque». Con el ceño fruncido funciona como pregunta: ¿por qué?", ["por qué", "porqué"], None, "¿POR-QUÉ?"),
    ("cuánto", "Preguntas", QW, ["cuánto", "cuanto"], None, "¿CUÁNTO?"),
    ("cuál", "Preguntas", QW, ["cuál", "cual"], None, "¿CUÁL?"),
    ("bebé", "Familia", "Recuerda el gesto de acunar a un bebé.", None, None, None),
    ("hijo", "Familia", "", None, None, None),
    ("abuelo", "Familia", "", None, None, None),
    ("hombre", "Personas", "", None, None, None),
    ("mujer", "Personas", "", None, None, None),
    ("niño", "Personas", "", None, None, None),
    ("novio", "Personas", "", None, None, None),
    ("vecino", "Personas", "", None, None, None),
    ("oficina", "Trabajo y estudio", "", None, None, None),
    ("ordenador", "Trabajo y estudio", "", None, None, None),
    ("cine", "Ocio", "", None, None, None),
    ("música", "Ocio", "", None, None, None),
    ("fútbol", "Ocio", "", None, None, None),
    ("nadar", "Ocio", "Recuerda el movimiento de los brazos al nadar.", None, None, None),
    ("correr", "Ocio", "", None, None, None),
    ("bailar", "Ocio", "", None, None, None),
    ("jugar", "Ocio", "", None, None, None),
    ("viajar", "Ocio", "", ["viajar", "viaje"], None, None),
    ("vacaciones", "Ocio", "", None, None, None),
    ("fiesta", "Ocio", "", None, None, None),
    ("cumpleaños", "Ocio", "", None, None, None),
    ("regalo", "Ocio", "", None, None, None),
    ("dolor", "Salud", "", None, None, None),
    ("cabeza", "Salud", "Las partes del cuerpo suelen signarse señalándolas.", None, None, None),
    ("enfermo", "Salud", "", None, None, None),
    ("medicina", "Salud", "", ["medicina", "medicamento", "pastilla"], None, None),
    ("ayudar", "Verbos", "Muchos verbos de LSE cambian de dirección según quién ayuda a quién.", ["ayudar", "ayuda"], None, None),
    ("esperar", "Verbos", "", None, None, None),
    ("necesitar", "Verbos", "", None, None, None),
    ("buscar", "Verbos", "", None, None, None),
    ("encontrar", "Verbos", "", None, None, None),
    ("pensar", "Verbos", "Se signa junto a la cabeza, donde están las ideas.", None, None, None),
    ("olvidar", "Verbos", "", None, None, None),
    ("recordar", "Verbos", "", ["recordar", "acordarse"], None, None),
    ("conocer", "Verbos", "", None, None, None),
    ("preguntar", "Verbos", "", ["preguntar", "pregunta"], None, None),
    ("contestar", "Verbos", "", ["contestar", "responder"], None, None),
    ("empezar", "Verbos", "", ["empezar", "comenzar"], None, None),
    ("terminar", "Verbos", "", ["terminar", "acabar"], None, None),
    ("llegar", "Verbos", "", None, None, None),
    ("salir", "Verbos", "", None, None, None),
    ("entrar", "Verbos", "", None, None, None),
    ("sentarse", "Verbos", "", ["sentar", "sentarse"], None, "SENTARSE"),
    ("levantarse", "Rutina", "", ["levantar", "levantarse"], None, "LEVANTARSE"),
    ("lunes", "Días", "", None, None, None),
    ("martes", "Días", "", None, None, None),
    ("miércoles", "Días", "", None, None, None),
    ("jueves", "Días", "", None, None, None),
    ("viernes", "Días", "", None, None, None),
    ("sábado", "Días", "", None, None, None),
    ("domingo", "Días", "", None, None, None),
    ("mes", "Tiempo", "", None, None, None),
    ("año", "Tiempo", "", None, None, None),
    ("hora", "Tiempo", "", None, None, None),
    ("tarde", "Tiempo", "Este es «la tarde», la parte del día. «Llegar tarde» se signa distinto: lo verás en «Llego tarde».", None, None, None),
    ("temprano", "Tiempo", "", None, None, None),
    ("siempre", "Tiempo", "", None, None, None),
    ("nunca", "Tiempo", "", None, None, None),
    ("también", "Básicos", "", None, None, None),
    ("igual", "Adjetivos", "", None, None, None),
    ("diferente", "Adjetivos", "", ["diferente", "distinto"], None, None),
    ("importante", "Adjetivos", "", None, None, None),
    ("verdad", "Básicos", "", None, None, None),
    ("mentira", "Básicos", "", None, None, None),
    ("problema", "Básicos", "", None, None, None),
    ("España", "Lugares", "", None, None, None),
]

# Signs used only inside expressions: key -> (label, queries)
EXTRA = {
    "mi": ("MI", ["mi", "mío"]),
    "que_tal": ("¿QUÉ-TAL?", ["qué tal"]),
    "encantado": ("ENCANTADO", ["encantado"]),
    "buenos_dias": ("BUENOS-DÍAS", ["buenos días"]),
    "buenas_tardes": ("BUENAS-TARDES", ["buenas tardes"]),
    "buenas_noches": ("BUENAS-NOCHES", ["buenas noches"]),
    "hasta_luego": ("HASTA-LUEGO", ["hasta luego"]),
    "hasta_manana": ("HASTA-MAÑANA", ["hasta mañana"]),
    "despacio": ("DESPACIO", ["despacio"]),
    "repetir": ("REPETIR", ["repetir"]),
    "de_nada": ("DE-NADA", ["de nada"]),
    "perdon": ("PERDÓN", ["perdón", "perdonar"]),
    "hambre": ("HAMBRE", ["hambre"]),
    "sed": ("SED", ["sed"]),
    "precio": ("PRECIO", ["precio"]),
    "momento": ("MOMENTO", ["momento"]),
    "amar": ("AMAR", ["amar", "te quiero"]),
    "enhorabuena": ("ENHORABUENA", ["enhorabuena", "felicidades", "felicitar"]),
    "quedar": ("QUEDAR", ["quedar", "cita"]),
    "nosotros": ("NOSOTROS", ["nosotros"]),
    "favorito": ("FAVORITO", ["favorito", "preferido"]),
    "parada": ("PARADA", ["parada"]),
    "comida": ("COMIDA", ["comida"]),
    "rico": ("RICO", ["rico", "sabroso"]),
    "tranquilo": ("TRANQUILO", ["tranquilo"]),
    "signo": ("SIGNO", ["signo"]),
    "significar": ("SIGNIFICAR", ["significar", "significado"]),
    "pareja": ("PAREJA", ["pareja"]),
    "dos": ("DOS", ["dos"]),
    "que_ha_pasado": ("¿QUÉ-HA-PASADO?", ["qué ha pasado"]),
    "encantar": ("ENCANTAR", ["encantar"]),
    "juntos": ("JUNTOS", ["juntos", "junto"]),
    "mensaje": ("MENSAJE", ["mensaje"]),
    "enviar": ("ENVIAR", ["enviar", "mandar"]),
    "fin_de_semana": ("FIN-DE-SEMANA", ["fin de semana"]),
    "bueno": ("BUENO", ["bueno"]),
    "estacion": ("ESTACIÓN", ["estación"]),
    "deber": ("DEBER", ["deber", "tener que"]),
    "ya": ("YA", ["ya"]),
    "cerca": ("CERCA", ["cerca"]),
    "lejos": ("LEJOS", ["lejos"]),
    "aqui": ("AQUÍ", ["aquí"]),
    "otro": ("OTRO", ["otro"]),
    "vez": ("VEZ", ["vez"]),
    "edad": ("EDAD", ["edad", "año"]),
    "tu_pos": ("TU", ["tu"]),
    "tarde_retraso": ("TARDE", ["tarde"]),
    "cuanto_dinero": ("¿CUÁNTO?", ["cuánto", "cuanto"]),
}

# Extra DILSE searches that only help to find the sign, without counting as a match: DILSE files
# expressions under their main word ("dias_buenos" is on the page of "día").
FIND = {
    "buenos_dias": ["día"],
    "buenas_tardes": ["tarde"],
    "buenas_noches": ["noche"],
    "hasta_luego": ["luego"],
    "hasta_manana": ["mañana"],
    "de_nada": ["nada"],
    "fin_de_semana": ["fin", "semana"],
    "por_favor": ["favor"],
    "gracias": ["gracia", "agradecer"],
    "que_ha_pasado": ["pasar"],
    "vacaciones": ["vacación"],
}

# DILSE file for signs whose automatic match is ambiguous (see the media-report branch).
FILES = {
    "gracias": "gracias_dar",
    "por_favor": "favor_por-pedir",
    "vacaciones": "vacacion",
    "musica": "musica_mon",
    "ya": "ya_e",
    "que_ha_pasado": "pasado-que-ha",
    "letra_ny": "n-fuerte",
    "letra_rr": "rr",
    "tu": "tu1",
    "tu_pos": "tu2",
    "hablar": "hablar-charlar_1",
    "lavar": "lavarse-manos",
    "poco": "poco_a",
    "momento": "momento-corto",
    "tarde": "tarde_a",
    "tarde_retraso": "tarde_c",
    "querer": "querer-desear",
    "poder": "poder-capacidad",
    "blanco": "blanco-color",
    "encantar": "encantar-gustar",
    "quedar": "quedar-cita",
    "rico": "rico-sabor",
    "estacion": "estacion-tren",
    "pequeno": "pequeno-cosa",
    "perdon": "perdon-educacion",
    "montana": "montana-naturaleza",
    "ano": "ano-meses",
    "edad": "ano-edad",
    "cuanto": "cuanto-numero",
    "cuanto_dinero": "cuanto-dinero",
    "que_tal": "que_tal",
    "buenas_tardes": "tardes_buenas",
    "buenas_noches": "noches_buenas",
    "hasta_manana": "manana_hasta",
}

# Fingerspelling alphabet: (letter, DILSE queries). The definition must mention "letra".
LETTERS = [
    ("A", ["a"]), ("B", ["b", "be"]), ("C", ["c", "ce"]), ("CH", ["ch", "che"]), ("D", ["d", "de"]),
    ("E", ["e"]), ("F", ["f", "efe"]), ("G", ["g", "ge"]), ("H", ["h", "hache"]), ("I", ["i"]),
    ("J", ["j", "jota"]), ("K", ["k", "ka"]), ("L", ["l", "ele"]), ("LL", ["ll", "elle"]), ("M", ["m", "eme"]),
    ("N", ["n", "ene"]), ("Ñ", ["ñ", "eñe"]), ("O", ["o"]), ("P", ["p", "pe"]), ("Q", ["q", "cu"]),
    ("R", ["r", "erre"]), ("RR", ["rr", "erre doble"]), ("S", ["s", "ese"]), ("T", ["t", "te"]), ("U", ["u"]),
    ("V", ["v", "uve"]), ("W", ["w", "uve doble"]), ("X", ["x", "equis"]), ("Y", ["y", "ye", "i griega"]),
    ("Z", ["z", "zeta"]),
]

NAME = "#(deletrea tu nombre)"
ORDER_V = "En LSE el verbo suele ir al final de la frase."
NO_SER = "En LSE no hace falta el verbo «ser» o «estar»: basta con poner juntas las dos ideas."
TIME_FIRST = "El tiempo (hoy, ayer, mañana…) se pone al principio y el verbo no cambia: no hay «fui» ni «iré»."
WH_END = "La palabra de la pregunta (¿qué?, ¿dónde?, ¿cuándo?…) suele ir al final, con el ceño fruncido."
YESNO = "En las preguntas de sí o no se levantan las cejas y se inclina un poco la cabeza hacia delante."
NEG_END = "La negación suele ir al final y se acompaña negando con la cabeza."
TOPIC = "Primero se dice de qué se habla (el tema) y después lo que se cuenta de ello."
INTENS = "La intensidad («muy», «mucho») se expresa con la cara y con un movimiento más marcado."

E = [
    ("Me llamo…", "Presentarse", ["mi", "nombre", NAME],
     "En LSE no hay verbo «llamarse»: se dice MI NOMBRE y se deletrea con el alfabeto dactilológico. Prueba a deletrear el tuyo en Biblioteca > Abecedario. Muchas personas sordas tienen también un signo-nombre."),
    ("¿Qué tal estás?", "Saludos", ["tu", "que_tal"], "Acompaña la pregunta con la cara: cejas y mirada hacia la otra persona."),
    ("¿Cómo te llamas?", "Presentarse", ["tu_pos", "nombre", "cual"], WH_END),
    ("Estoy bien, gracias", "Saludos", ["yo", "bien", "gracias"], NO_SER),
    ("Buenos días", "Saludos", ["buenos_dias"], "Es un saludo con su propio signo; acompáñalo con una sonrisa."),
    ("¿Eres sordo?", "Presentarse", ["tu", "sordo"], YESNO),
    ("Soy oyente", "Presentarse", ["yo", "oyente"], NO_SER),
    ("Estoy aprendiendo lengua de signos", "Comunicación", ["yo", "signar", "aprender"], ORDER_V),
    ("¿Hablas lengua de signos?", "Comunicación", ["tu", "signar"], YESNO),
    ("Signo un poco", "Comunicación", ["yo", "signar", "poco"], "«Un poco» se puede reforzar con la cara: labios apretados y ojos entornados."),
    ("No entiendo", "Comunicación", ["yo", "entender", "no"], NEG_END),
    ("Más despacio, por favor", "Comunicación", ["despacio", "por_favor"], "Muy útil al principio: pedirlo es lo normal y nadie se molesta."),
    ("Encantado de conocerte", "Presentarse", ["encantado"], "Acompáñalo de una sonrisa y mirando a los ojos: en la comunidad sorda mantener la mirada es importante."),
    ("Hasta luego", "Saludos", ["hasta_luego"], ""),
    ("¿Dónde vives?", "Presentarse", ["tu", "vivir", "donde"], WH_END),
    ("Vivo en…", "Presentarse", ["yo", "vivir", "#(signo de tu ciudad)"], "Muchas ciudades tienen su propio signo. Si no lo sabes, deletrea el nombre."),
    ("Gracias. De nada", "Cortesía", ["gracias", "de_nada"], ""),
    ("Buenas noches", "Saludos", ["buenas_noches"], ""),
    ("¿Puedes repetir, por favor?", "Comunicación", ["repetir", "por_favor"], YESNO),
    ("Me voy a duchar", "Rutina", ["yo", "ducharse", "ir"], ORDER_V),
    ("Tengo coche", "Presentarse", ["yo", "coche", "tener"], ORDER_V),
    ("Voy a casa", "Rutina", ["yo", "casa", "ir"], ORDER_V),
    ("Tengo hambre", "Comida", ["yo", "hambre"], NO_SER),
    ("Tengo sed", "Comida", ["yo", "sed"], NO_SER),
    ("Estoy cansado", "Emociones", ["yo", "cansado"], NO_SER + " La cara de cansancio forma parte del signo."),
    ("Estoy contento", "Emociones", ["yo", "feliz"], NO_SER),
    ("Me gusta el café", "Comida", ["cafe", "yo", "gustar"], TOPIC),
    ("No me gusta", "Comida", ["gustar", "no"], NEG_END + " Algunos verbos tienen además su propio signo negativo."),
    ("¿Tienes hermanos?", "Familia", ["tu", "hermano", "tener"], YESNO),
    ("Mi familia es grande", "Familia", ["mi", "familia", "grande"], NO_SER),
    ("Hoy hace frío", "Tiempo atmosférico", ["hoy", "frio"], TIME_FIRST),
    ("Mañana va a llover", "Tiempo atmosférico", ["manana", "llover"], TIME_FIRST),
    ("Hoy hace sol", "Tiempo atmosférico", ["hoy", "sol"], TIME_FIRST),
    ("¿Qué hora es?", "Tiempo", ["hora", "que"], WH_END),
    ("¿Cuántos años tienes?", "Presentarse", ["tu_pos", "edad", "cuanto"], WH_END),
    ("¿Dónde está el baño?", "Lugares", ["bano", "donde"], WH_END),
    ("¿Qué quieres comer?", "Comida", ["tu", "comer", "querer", "que"], WH_END),
    ("Quiero agua, por favor", "Comida", ["agua", "querer", "por_favor"], TOPIC),
    ("¿Quieres un café?", "Comida", ["tu", "cafe", "querer"], YESNO),
    ("¿Cuánto cuesta?", "Compras", ["precio", "cuanto_dinero"], WH_END),
    ("Es muy caro", "Compras", ["caro"], INTENS),
    ("¿En qué trabajas?", "Trabajo y estudio", ["tu", "trabajar", "que"], WH_END),
    ("Trabajo en una oficina", "Trabajo y estudio", ["yo", "oficina", "trabajar"], ORDER_V),
    ("¿Qué estudias?", "Trabajo y estudio", ["tu", "estudiar", "que"], WH_END),
    ("Me duele la cabeza", "Salud", ["cabeza", "dolor"], "El dolor se signa señalando la parte del cuerpo que duele, con cara de dolor."),
    ("Necesito un médico", "Salud", ["medico", "necesitar"], TOPIC),
    ("¿Me ayudas?", "Comunicación", ["ayudar", "poder"], "AYUDAR cambia de dirección: va de la otra persona hacia ti. " + YESNO),
    ("Espera un momento", "Comunicación", ["momento", "esperar"], ""),
    ("Te quiero", "Emociones", ["yo", "amar", "tu"], "Hay varias formas de decirlo. También verás el signo internacional con pulgar, índice y meñique extendidos."),
    ("Feliz cumpleaños", "Ocio", ["cumpleanos", "feliz"], TOPIC),
    ("¡Enhorabuena!", "Cortesía", ["enhorabuena"], INTENS),
    ("¿Qué hiciste ayer?", "Ocio", ["ayer", "tu", "hacer", "que"], TIME_FIRST),
    ("Ayer fui al cine", "Ocio", ["ayer", "yo", "cine", "ir"], TIME_FIRST),
    ("¿Quedamos el sábado?", "Ocio", ["sabado", "quedar"], TIME_FIRST + " " + YESNO),
    ("Me gusta viajar", "Ocio", ["viajar", "yo", "gustar"], TOPIC),
    ("¿Te gusta el fútbol?", "Ocio", ["futbol", "tu", "gustar"], TOPIC + " " + YESNO),
    ("Estoy de vacaciones", "Ocio", ["yo", "vacaciones"], NO_SER),
    ("Vamos a la playa", "Ocio", ["nosotros", "playa", "ir"], ORDER_V),
    ("Tengo un perro", "Animales", ["yo", "perro", "tener"], ORDER_V),
    ("¿De qué color es?", "Colores", ["color", "cual"], WH_END),
    ("Mi color favorito es el azul", "Colores", ["mi", "color", "favorito", "azul"], NO_SER),
    ("¿Dónde está la parada del autobús?", "Transporte", ["autobus", "parada", "donde"], WH_END),
    ("Voy en tren", "Transporte", ["yo", "tren", "ir"], ORDER_V),
    ("Llego tarde", "Tiempo", ["yo", "llegar", "tarde_retraso"], ""),
    ("¿A qué hora empieza?", "Tiempo", ["empezar", "hora", "que"], WH_END),
    ("Ya he terminado", "Tiempo", ["terminar", "ya"], "El verbo no cambia con el tiempo: «ya» indica que la acción está acabada."),
    ("¿Dónde están las llaves?", "La casa", ["llave", "donde"], WH_END),
    ("La comida está muy rica", "Comida", ["comida", "rico"], NO_SER + " " + INTENS),
    ("Desayuno café con leche", "Comida", ["yo", "desayunar", "cafe", "leche"], ""),
    ("Estoy enfermo", "Salud", ["yo", "enfermo"], NO_SER),
    ("Tengo miedo", "Emociones", ["yo", "miedo"], NO_SER + " La cara de miedo forma parte del signo."),
    ("Tranquilo, no pasa nada", "Emociones", ["tranquilo"], "Acompáñalo con una cara relajada y un gesto suave."),
    ("¿Qué significa este signo?", "Comunicación", ["signo", "significar", "que"], WH_END),
    ("¿Cómo se signa…?", "Comunicación", ["#(la palabra)", "signar", "como"], "Pregunta muy útil: primero la palabra (deletreada o señalada) y al final ¿CÓMO?"),
    ("Soy de España", "Presentarse", ["yo", "espana"], NO_SER),
    ("¿Estás bien?", "Saludos", ["tu", "bien"], YESNO),
    ("Tengo una pregunta", "Comunicación", ["yo", "preguntar"], ""),
    ("No lo sé", "Comunicación", ["saber", "no"], NEG_END + " Algunos verbos tienen además su propio signo negativo."),
    ("Se me ha olvidado", "Comunicación", ["yo", "olvidar"], ""),
    ("¿Te acuerdas?", "Comunicación", ["tu", "recordar"], YESNO),
    ("Hoy hace calor", "Tiempo atmosférico", ["hoy", "calor"], TIME_FIRST),
    ("Está nevando", "Tiempo atmosférico", ["ahora", "nieve"], TIME_FIRST),
    ("¿Cuándo es tu cumpleaños?", "Ocio", ["tu", "cumpleanos", "cuando"], WH_END),
    ("Mi madre se llama…", "Familia", ["mi", "madre", "nombre", "#(deletrea su nombre)"], "Igual que al presentarte: MI MADRE NOMBRE y se deletrea."),
    ("Tengo dos hijos", "Familia", ["yo", "hijo", "dos"], "Los números suelen ir detrás de lo que se cuenta."),
    ("Vivo con mi pareja", "Familia", ["yo", "pareja", "vivir"], ORDER_V),
    ("¿Qué ha pasado?", "Emociones", ["que_ha_pasado"], "Es un solo signo. Acompáñalo con cara de sorpresa o preocupación: la expresión cuenta."),
    ("Me encanta bailar", "Ocio", ["bailar", "yo", "encantar"], TOPIC),
    ("Vamos juntos", "Ocio", ["nosotros", "juntos", "ir"], ORDER_V),
    ("Mándame un mensaje", "Comunicación", ["mensaje", "enviar"], "ENVIAR cambia de dirección: va de la otra persona hacia ti."),
    ("Buen fin de semana", "Saludos", ["fin_de_semana", "bueno"], "El adjetivo suele ir detrás del nombre."),
    ("¡Qué bonito!", "Emociones", ["bonito"], "Las exclamaciones se marcan con la cara: ojos abiertos y expresión de sorpresa."),
    ("Perdona, ¿dónde está la estación?", "Transporte", ["perdon", "estacion", "donde"], WH_END),
    ("No hay problema", "Básicos", ["problema", "no"], NEG_END),
    ("Tengo que irme", "Básicos", ["yo", "ir", "deber"], ORDER_V),
    ("¿Me lo puedes escribir?", "Comunicación", ["escribir", "poder"], "Muy útil para entenderse entre personas sordas y oyentes. " + YESNO),
    ("¿Dónde trabajas?", "Trabajo y estudio", ["tu", "trabajar", "donde"], WH_END),
    ("Buenas tardes", "Saludos", ["buenas_tardes"], ""),
    ("Hasta mañana", "Saludos", ["hasta_manana"], ""),
    ("¿Está cerca?", "Lugares", ["cerca"], YESNO),
    ("Está lejos", "Lugares", ["lejos"], INTENS + " Cuanto más lejos, más largo el movimiento."),
    ("Vivo aquí", "Lugares", ["yo", "vivir", "aqui"], ""),
    ("Otra vez", "Comunicación", ["otro", "vez"], ""),
    ("¿Quién es?", "Personas", ["quien"], WH_END),
    ("Mi amigo es sordo", "Personas", ["mi", "amigo", "sordo"], NO_SER),
    ("¿Por qué?", "Preguntas", ["por_que"], WH_END),
    ("Siempre llego temprano", "Tiempo", ["yo", "siempre", "temprano", "llegar"], ""),
    ("Nunca como carne", "Comida", ["carne", "yo", "comer", "nunca"], TOPIC + " " + NEG_END),
    ("Me gusta leer libros", "Ocio", ["libro", "leer", "yo", "gustar"], TOPIC),
    ("¿Vienes a la fiesta?", "Ocio", ["fiesta", "tu", "venir"], TOPIC + " " + YESNO),
    ("Es muy importante", "Básicos", ["importante"], INTENS),
    ("Es verdad", "Básicos", ["verdad"], ""),
    ("Mi abuelo vive en un pueblo", "Familia", ["mi", "abuelo", "pueblo", "vivir"], ORDER_V),
    ("Mi gato es negro", "Animales", ["mi", "gato", "negro"], NO_SER),
    ("Me levanto a las siete", "Rutina", ["yo", "levantarse", "#(hora: SIETE)"], "Las horas se dicen con el signo HORA y el número."),
    ("Por la noche leo", "Rutina", ["noche", "yo", "leer"], TIME_FIRST),
    ("Voy al médico", "Salud", ["yo", "medico", "ir"], ORDER_V),
    ("Tengo frío", "Tiempo atmosférico", ["yo", "frio"], NO_SER),
    ("Es fácil", "Básicos", ["facil"], ""),
    ("Es difícil", "Básicos", ["dificil"], "Pon cara de esfuerzo: la expresión refuerza el signo."),
    ("¿Dónde está la tienda?", "Compras", ["tienda", "donde"], WH_END),
    ("Quiero comprar pan", "Compras", ["pan", "comprar", "querer"], TOPIC),
    ("¿Conoces a mi hermano?", "Familia", ["mi", "hermano", "tu", "conocer"], TOPIC + " " + YESNO),
    ("Estoy nervioso", "Emociones", ["yo", "nervioso"], NO_SER),
    ("Estoy triste", "Emociones", ["yo", "triste"], NO_SER),
    ("Estoy enfadado", "Emociones", ["yo", "enfadado"], NO_SER),
    ("Hoy es lunes", "Días", ["hoy", "lunes"], NO_SER),
    ("Nos vemos el domingo", "Días", ["domingo", "nosotros", "quedar"], TIME_FIRST),
    ("Esta semana trabajo mucho", "Trabajo y estudio", ["semana", "yo", "trabajar", "mucho"], TIME_FIRST),
    ("Mañana no trabajo", "Trabajo y estudio", ["manana", "yo", "trabajar", "no"], TIME_FIRST + " " + NEG_END),
]


def main(out_path):
    words = []
    keys = {}
    for w, cat, tip, q, key, label in W:
        key = key or slug(w)
        assert key not in keys, f"duplicate key {key}"
        label = label or w.upper().replace(" ", "-")
        item = {"w": w, "cat": cat, "key": key, "label": label, "q": q or [w]}
        if tip:
            item["tip"] = tip
        if key in FILES:
            item["file"] = FILES[key]
        if key in FIND:
            item["find"] = FIND[key]
        keys[key] = label
        words.append(item)
    extra = {}
    for key, (label, q) in EXTRA.items():
        assert key not in keys, f"extra key {key} already a word"
        keys[key] = label
        extra[key] = {"label": label, "q": q}
        if key in FILES:
            extra[key]["file"] = FILES[key]
        if key in FIND:
            extra[key]["find"] = FIND[key]
    letters = []
    for letter, q in LETTERS:
        key = "letra_" + slug(letter.replace("Ñ", "ny"))
        assert key not in keys, f"duplicate key {key}"
        keys[key] = letter
        item = {"key": key, "label": letter, "q": q, "sense": "letra"}
        if key in FILES:
            item["file"] = FILES[key]
        letters.append(item)
    used = set()
    exprs = []
    for es, cat, signs, note in E:
        for s in signs:
            if not s.startswith("#"):
                assert s in keys, f"unknown sign {s} in {es}"
                used.add(s)
        item = {"es": es, "cat": cat, "signs": signs}
        if note:
            item["note"] = note
        exprs.append(item)
    assert not set(FILES) - set(keys), f"FILES for unknown keys: {set(FILES) - set(keys)}"
    assert not set(FIND) - set(keys), f"FIND for unknown keys: {set(FIND) - set(keys)}"
    unused_extra = set(extra) - used
    assert not unused_extra, f"unused extra signs: {unused_extra}"
    heads = [e["es"] for e in exprs]
    assert len(heads) == len(set(heads)), "duplicate expressions"

    lines = ["{", '  "source": "Vídeos: DILSE, Fundación CNSE (CC BY-NC-SA 3.0). Glosas y notas: Signos Daily.",', '  "words": [']
    lines.append(",\n".join("    " + json.dumps(x, ensure_ascii=False) for x in words))
    lines.append("  ],")
    lines.append('  "signs": {')
    lines.append(",\n".join(f"    {json.dumps(k)}: {json.dumps(v, ensure_ascii=False)}" for k, v in extra.items()))
    lines.append("  },")
    lines.append('  "letters": [')
    lines.append(",\n".join("    " + json.dumps(x, ensure_ascii=False) for x in letters))
    lines.append("  ],")
    lines.append('  "expressions": [')
    lines.append(",\n".join("    " + json.dumps(x, ensure_ascii=False) for x in exprs))
    lines.append("  ]")
    lines.append("}")
    text = "\n".join(lines) + "\n"
    json.loads(text)
    open(out_path, "w", encoding="utf-8").write(text)
    print(f"{len(words)} words, {len(extra)} extra signs, {len(letters)} letters, {len(exprs)} expressions, {len(keys)} media keys")
    words_in_expr = {s for e in exprs for s in e["signs"] if not s.startswith("#")} & {w["key"] for w in words}
    print(f"{len(words_in_expr)} words appear in expressions")


if __name__ == "__main__":
    main(sys.argv[1])
