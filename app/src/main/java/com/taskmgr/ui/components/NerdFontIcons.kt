package com.taskmgr.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.taskmgr.ui.theme.UbuntuSansNerdFontFamily

private fun nerdGlyph(codePoint: Int): String = String(Character.toChars(codePoint))

/**
 * Verified Nerd Font v3 Material Design glyphs bundled inside ubuntu_sans_nerd_regular.ttf.
 */
object NerdIcons {
    val Airplane = nerdGlyph(0xF001D)
    val Alarm = nerdGlyph(0xF0020)
    val AlertCircle = nerdGlyph(0xF0028)
    val Archive = nerdGlyph(0xF003C)
    val ArmFlex = nerdGlyph(0xF0FE7)
    val Atom = nerdGlyph(0xF0768)
    val BabyCarriage = nerdGlyph(0xF138E)
    val BagPersonal = nerdGlyph(0xF0E13)
    val Bank = nerdGlyph(0xF0070)
    val Basketball = nerdGlyph(0xF0806)
    val Bath = nerdGlyph(0xF007D)
    val BatteryCharging = nerdGlyph(0xF0084)
    val BatteryHigh = nerdGlyph(0xF0080)
    val Beach = nerdGlyph(0xF0092)
    val Bed = nerdGlyph(0xF02E3)
    val Beer = nerdGlyph(0xF0098)
    val BellAlert = nerdGlyph(0xF0D59)
    val BellOffOutline = nerdGlyph(0xF0A91)
    val BellRing = nerdGlyph(0xF009E)
    val Bible = nerdGlyph(0xF00A1)
    val Bike = nerdGlyph(0xF00A3)
    val Binoculars = nerdGlyph(0xF00A5)
    val Blender = nerdGlyph(0xF0CE1)
    val Book = nerdGlyph(0xF00BA)
    val Bookmark = nerdGlyph(0xF00C0)
    val BookOpen = nerdGlyph(0xF00BD)
    val BottleTonic = nerdGlyph(0xF112E)
    val BowlMix = nerdGlyph(0xF0617)
    val BoxingGlove = nerdGlyph(0xF0B6C)
    val Brain = nerdGlyph(0xF09D1)
    val BreadSlice = nerdGlyph(0xF0CEE)
    val Briefcase = nerdGlyph(0xF00D6)
    val Broom = nerdGlyph(0xF00E2)
    val Brush = nerdGlyph(0xF00E3)
    val Bucket = nerdGlyph(0xF1415)
    val Bus = nerdGlyph(0xF00E7)
    val Cake = nerdGlyph(0xF00EA)
    val Calculator = nerdGlyph(0xF00EC)
    val Calendar = nerdGlyph(0xF00ED)
    val CalendarCheck = nerdGlyph(0xF00EF)
    val CalendarToday = nerdGlyph(0xF00F6)
    val Call = nerdGlyph(0xF03F2)
    val Camera = nerdGlyph(0xF0100)
    val Campfire = nerdGlyph(0xF0EDD)
    val Candle = nerdGlyph(0xF05E2)
    val Candy = nerdGlyph(0xF00F9)
    val Car = nerdGlyph(0xF010B)
    val Cards = nerdGlyph(0xF0638)
    val Carrot = nerdGlyph(0xF0F4F)
    val Cart = nerdGlyph(0xF0110)
    val Cash = nerdGlyph(0xF0114)
    val Cat = nerdGlyph(0xF011B)
    val Cellphone = nerdGlyph(0xF011C)
    val Chat = nerdGlyph(0xF0B79)
    val CheckboxBlank = nerdGlyph(0xF0131)
    val CheckboxMarkedCircle = nerdGlyph(0xF0133)
    val CheckboxMarkedOutline = nerdGlyph(0xF0134)
    val CheckCircle = nerdGlyph(0xF05E0)
    val ChefHat = nerdGlyph(0xF0B74)
    val Chess = nerdGlyph(0xF0856)
    val Church = nerdGlyph(0xF0144)
    val City = nerdGlyph(0xF0146)
    val ClipboardCheck = nerdGlyph(0xF014E)
    val ClockCheck = nerdGlyph(0xF0FA9)
    val ClockFast = nerdGlyph(0xF0151)
    val ClockOutline = nerdGlyph(0xF0150)
    val Close = nerdGlyph(0xF0156)
    val ClothesHanger = nerdGlyph(0xF02B8)
    val Cloud = nerdGlyph(0xF015F)
    val CodeTags = nerdGlyph(0xF0174)
    val Coffee = nerdGlyph(0xF0176)
    val CoffeeMaker = nerdGlyph(0xF1099)
    val Cog = nerdGlyph(0xF0493)
    val Compass = nerdGlyph(0xF018B)
    val Controller = nerdGlyph(0xF02B4)
    val Cookie = nerdGlyph(0xF0198)
    val Counter = nerdGlyph(0xF019C)
    val CreditCard = nerdGlyph(0xF019B)
    val Crown = nerdGlyph(0xF01A5)
    val Cup = nerdGlyph(0xF01A8)
    val CupWater = nerdGlyph(0xF019F)
    val DeleteOutline = nerdGlyph(0xF09E7)
    val Desk = nerdGlyph(0xF1239)
    val Diamond = nerdGlyph(0xF01C8)
    val Dice = nerdGlyph(0xF076E)
    val Dishwasher = nerdGlyph(0xF0AA0)
    val Doctor = nerdGlyph(0xF0A06)
    val Dog = nerdGlyph(0xF0A44)
    val Draw = nerdGlyph(0xF0F59)
    val Dumbbell = nerdGlyph(0xF0206)
    val EarHearing = nerdGlyph(0xF07C5)
    val Earth = nerdGlyph(0xF01E7)
    val Email = nerdGlyph(0xF01EE)
    val EmoticonHappy = nerdGlyph(0xF01F2)
    val Eye = nerdGlyph(0xF0208)
    val FaceMask = nerdGlyph(0xF1586)
    val Fan = nerdGlyph(0xF0210)
    val Feather = nerdGlyph(0xF06D4)
    val FileDocument = nerdGlyph(0xF0219)
    val Film = nerdGlyph(0xF0229)
    val Fire = nerdGlyph(0xF0238)
    val Fish = nerdGlyph(0xF023A)
    val Flag = nerdGlyph(0xF023B)
    val Flash = nerdGlyph(0xF0241)
    val Flower = nerdGlyph(0xF024A)
    val Folder = nerdGlyph(0xF024B)
    val Food = nerdGlyph(0xF025A)
    val FoodApple = nerdGlyph(0xF025B)
    val Football = nerdGlyph(0xF0260)
    val FormatListChecks = nerdGlyph(0xF0279)
    val FountainPen = nerdGlyph(0xF0D10)
    val Fridge = nerdGlyph(0xF0290)
    val FruitCherries = nerdGlyph(0xF1042)
    val FruitWatermelon = nerdGlyph(0xF104A)
    val Gift = nerdGlyph(0xF02A1)
    val Glasses = nerdGlyph(0xF02AA)
    val Golf = nerdGlyph(0xF0822)
    val GuitarAcoustic = nerdGlyph(0xF0770)
    val Hammer = nerdGlyph(0xF08E9)
    val HandHeart = nerdGlyph(0xF10F2)
    val HandWash = nerdGlyph(0xF157F)
    val Headphones = nerdGlyph(0xF02CB)
    val Heart = nerdGlyph(0xF02D1)
    val HeartPulse = nerdGlyph(0xF05F6)
    val Hiking = nerdGlyph(0xF0D7B)
    val History = nerdGlyph(0xF02DA)
    val Home = nerdGlyph(0xF02DC)
    val HomeOutline = nerdGlyph(0xF06A1)
    val Hospital = nerdGlyph(0xF0FF6)
    val Hourglass = nerdGlyph(0xF051F)
    val IceCream = nerdGlyph(0xF082A)
    val Image = nerdGlyph(0xF02E9)
    val JumpRope = nerdGlyph(0xF12FF)
    val Karate = nerdGlyph(0xF082C)
    val Kettle = nerdGlyph(0xF05FA)
    val Key = nerdGlyph(0xF0306)
    val Keyboard = nerdGlyph(0xF030C)
    val Lamp = nerdGlyph(0xF06B5)
    val Language = nerdGlyph(0xF031A)
    val Laptop = nerdGlyph(0xF0322)
    val Laundry = nerdGlyph(0xF072A)
    val Layers = nerdGlyph(0xF0328)
    val Leaf = nerdGlyph(0xF032A)
    val Lightbulb = nerdGlyph(0xF0335)
    val Lipstick = nerdGlyph(0xF13B5)
    val Lock = nerdGlyph(0xF033E)
    val LockOpenOutline = nerdGlyph(0xF0340)
    val Lotion = nerdGlyph(0xF1582)
    val Mailbox = nerdGlyph(0xF06EE)
    val Map = nerdGlyph(0xF034D)
    val Meditation = nerdGlyph(0xF0986)
    val Microphone = nerdGlyph(0xF036C)
    val Microwave = nerdGlyph(0xF0C99)
    val Monitor = nerdGlyph(0xF0379)
    val Moon = nerdGlyph(0xF0367)
    val Mop = nerdGlyph(0xF1534)
    val Motorbike = nerdGlyph(0xF037C)
    val Movie = nerdGlyph(0xF0381)
    val Mug = nerdGlyph(0xF0386)
    val Music = nerdGlyph(0xF075A)
    val MusicNote = nerdGlyph(0xF0387)
    val Nail = nerdGlyph(0xF0DDC)
    val Needle = nerdGlyph(0xF052E)
    val Newspaper = nerdGlyph(0xF0395)
    val Noodles = nerdGlyph(0xF117E)
    val Notebook = nerdGlyph(0xF082E)
    val Nutrition = nerdGlyph(0xF03A6)
    val Oil = nerdGlyph(0xF03B4)
    val Origami = nerdGlyph(0xF13B8)
    val Package = nerdGlyph(0xF03D3)
    val Paintbrush = nerdGlyph(0xF03D7)
    val Palette = nerdGlyph(0xF03D8)
    val Paperclip = nerdGlyph(0xF03E2)
    val Paw = nerdGlyph(0xF03E9)
    val Pencil = nerdGlyph(0xF03EB)
    val Piano = nerdGlyph(0xF067C)
    val PiggyBank = nerdGlyph(0xF1002)
    val Pill = nerdGlyph(0xF0402)
    val Pizza = nerdGlyph(0xF040F)
    val Plane = nerdGlyph(0xF0410)
    val Plant = nerdGlyph(0xF1166)
    val Plus = nerdGlyph(0xF0415)
    val PlusCircle = nerdGlyph(0xF0419)
    val Podcast = nerdGlyph(0xF0D1B)
    val Pool = nerdGlyph(0xF0606)
    val Pot = nerdGlyph(0xF02C0)
    val Prayer = nerdGlyph(0xF131E)
    val Printer = nerdGlyph(0xF042A)
    val Puzzle = nerdGlyph(0xF0431)
    val Radio = nerdGlyph(0xF0439)
    val Recycle = nerdGlyph(0xF044C)
    val Repeat = nerdGlyph(0xF0456)
    val Rice = nerdGlyph(0xF07EA)
    val Road = nerdGlyph(0xF0461)
    val Robot = nerdGlyph(0xF06A9)
    val RocketLaunch = nerdGlyph(0xF14DE)
    val RoomService = nerdGlyph(0xF088D)
    val Ruler = nerdGlyph(0xF0475)
    val Run = nerdGlyph(0xF070E)
    val Sailing = nerdGlyph(0xF09B0)
    val ScaleBathroom = nerdGlyph(0xF0472)
    val School = nerdGlyph(0xF0474)
    val Scissors = nerdGlyph(0xF0480)
    val Seed = nerdGlyph(0xF0E65)
    val Sewing = nerdGlyph(0xF0EE9)
    val Shield = nerdGlyph(0xF0498)
    val ShieldCheck = nerdGlyph(0xF0565)
    val ShoeSneaker = nerdGlyph(0xF09B2)
    val Shopping = nerdGlyph(0xF049B)
    val Shovel = nerdGlyph(0xF0710)
    val Shower = nerdGlyph(0xF09A0)
    val Silverware = nerdGlyph(0xF04A6)
    val Skate = nerdGlyph(0xF0D25)
    val Ski = nerdGlyph(0xF1304)
    val Sleep = nerdGlyph(0xF04B2)
    val Soccer = nerdGlyph(0xF04B8)
    val Sofa = nerdGlyph(0xF04B9)
    val Speaker = nerdGlyph(0xF04C3)
    val Sprout = nerdGlyph(0xF0E66)
    val Stairs = nerdGlyph(0xF12B1)
    val Star = nerdGlyph(0xF04CE)
    val Stethoscope = nerdGlyph(0xF04E0)
    val Stove = nerdGlyph(0xF04E6)
    val Sun = nerdGlyph(0xF05A8)
    val Sunset = nerdGlyph(0xF059B)
    val Swim = nerdGlyph(0xF04F1)
    val Table = nerdGlyph(0xF04F7)
    val Tag = nerdGlyph(0xF04F9)
    val Target = nerdGlyph(0xF04FE)
    val Tea = nerdGlyph(0xF0D9F)
    val Telescope = nerdGlyph(0xF0B3B)
    val Tennis = nerdGlyph(0xF0D2C)
    val Tent = nerdGlyph(0xF050B)
    val Thermometer = nerdGlyph(0xF050F)
    val Ticket = nerdGlyph(0xF0516)
    val Timer = nerdGlyph(0xF13AB)
    val TimerOutline = nerdGlyph(0xF051B)
    val Toilet = nerdGlyph(0xF09AB)
    val Tools = nerdGlyph(0xF1064)
    val Tooth = nerdGlyph(0xF08C3)
    val Toothbrush = nerdGlyph(0xF1129)
    val Train = nerdGlyph(0xF0533)
    val Trash = nerdGlyph(0xF0A7A)
    val Tree = nerdGlyph(0xF0531)
    val Trophy = nerdGlyph(0xF0538)
    val Truck = nerdGlyph(0xF053D)
    val TshirtCrew = nerdGlyph(0xF0A7B)
    val Tune = nerdGlyph(0xF062E)
    val Tv = nerdGlyph(0xF0502)
    val Umbrella = nerdGlyph(0xF054A)
    val Vacuum = nerdGlyph(0xF19A0)
    val Vibrate = nerdGlyph(0xF0566)
    val Video = nerdGlyph(0xF0567)
    val Violin = nerdGlyph(0xF060F)
    val Walk = nerdGlyph(0xF0583)
    val Wallet = nerdGlyph(0xF0584)
    val Watch = nerdGlyph(0xF058B)
    val Water = nerdGlyph(0xF058C)
    val WateringCan = nerdGlyph(0xF1481)
    val WeatherPartlyCloudy = nerdGlyph(0xF0595)
    val WeatherRain = nerdGlyph(0xF0596)
    val WeatherSnow = nerdGlyph(0xF0598)
    val Weight = nerdGlyph(0xF05A1)
    val WeightLifter = nerdGlyph(0xF115D)
    val Wheelchair = nerdGlyph(0xF05B2)
    val Wifi = nerdGlyph(0xF05A9)
    val Window = nerdGlyph(0xF05AF)
    val Wine = nerdGlyph(0xF02B0)
    val Wrench = nerdGlyph(0xF05B7)
    val Yoga = nerdGlyph(0xF117C)
}

data class NerdIconOption(
    val label: String,
    val category: String,
    val glyph: String,
    val keywords: List<String> = emptyList()
)

object NerdIconCatalog {
    val all: List<NerdIconOption> = listOf(
        // Casa & Rotina Doméstica
        NerdIconOption("Casa", "Casa", NerdIcons.Home, listOf("home", "lar", "moradia", "apartamento")),
        NerdIconOption("Casa Contorno", "Casa", NerdIcons.HomeOutline, listOf("home", "lar")),
        NerdIconOption("Vassoura / Limpeza", "Casa", NerdIcons.Broom, listOf("varrer", "faxina", "limpar", "arrumar")),
        NerdIconOption("Esfregão / Pano", "Casa", NerdIcons.Mop, listOf("limpeza", "chao", "faxina", "mop")),
        NerdIconOption("Aspirador", "Casa", NerdIcons.Vacuum, listOf("limpeza", "poeira", "aspirar")),
        NerdIconOption("Balde", "Casa", NerdIcons.Bucket, listOf("limpeza", "agua", "lavar")),
        NerdIconOption("Lixo / Reciclar", "Casa", NerdIcons.Trash, listOf("lixeira", "descartar", "tirar lixo")),
        NerdIconOption("Reciclagem", "Casa", NerdIcons.Recycle, listOf("reciclar", "sustentavel", "lixo")),
        NerdIconOption("Máquina de Lavar / Roupa", "Casa", NerdIcons.Laundry, listOf("lavanderia", "roupas", "lavar")),
        NerdIconOption("Cabide / Guarda-roupa", "Casa", NerdIcons.ClothesHanger, listOf("roupa", "armario", "organizar")),
        NerdIconOption("Camiseta / Vestir", "Casa", NerdIcons.TshirtCrew, listOf("roupa", "dobrar")),
        NerdIconOption("Lava-louças / Louça", "Casa", NerdIcons.Dishwasher, listOf("cozinha", "pratos", "lavar louca")),
        NerdIconOption("Sofá / Sala", "Casa", NerdIcons.Sofa, listOf("sala", "descanso", "organizar")),
        NerdIconOption("Mesa / Jantar", "Casa", NerdIcons.Table, listOf("movel", "escritorio", "sala")),
        NerdIconOption("Janela", "Casa", NerdIcons.Window, listOf("ventilar", "abrir", "cortina")),
        NerdIconOption("Abajur / Luz", "Casa", NerdIcons.Lamp, listOf("iluminacao", "quarto", "noite")),
        NerdIconOption("Chave / Porta", "Casa", NerdIcons.Key, listOf("trancar", "seguranca", "sair")),
        NerdIconOption("Cadeado / Trancar", "Casa", NerdIcons.Lock, listOf("seguranca", "porta", "privacidade")),
        NerdIconOption("Cozinha / Talheres", "Casa", NerdIcons.Silverware, listOf("comer", "cozinhar", "refeicao")),
        NerdIconOption("Fogão / Cozinhar", "Casa", NerdIcons.Stove, listOf("cozinha", "panela", "preparo")),
        NerdIconOption("Panela", "Casa", NerdIcons.Pot, listOf("cozinhar", "almoço", "janta")),
        NerdIconOption("Micro-ondas", "Casa", NerdIcons.Microwave, listOf("esquentar", "cozinha")),
        NerdIconOption("Geladeira", "Casa", NerdIcons.Fridge, listOf("mercado", "cozinha", "alimentos")),
        NerdIconOption("Liquidificador", "Casa", NerdIcons.Blender, listOf("vitamina", "suco", "shake")),
        NerdIconOption("Chaleira", "Casa", NerdIcons.Kettle, listOf("cha", "cafe", "ferver")),
        NerdIconOption("Cafeteira", "Casa", NerdIcons.CoffeeMaker, listOf("cafe", "manha")),
        NerdIconOption("Chef / Culinária", "Casa", NerdIcons.ChefHat, listOf("cozinhar", "receita", "cozinha")),
        NerdIconOption("Ventilador / Ar", "Casa", NerdIcons.Fan, listOf("quarto", "ar", "limpeza")),
        NerdIconOption("Caminhão / Mudança", "Casa", NerdIcons.Truck, listOf("entrega", "frete", "transporte")),
        NerdIconOption("Semente / Plantar", "Casa", NerdIcons.Seed, listOf("horta", "jardim", "plantio")),
        NerdIconOption("Costura / Linha", "Casa", NerdIcons.Sewing, listOf("roupa", "remendo", "artesanato")),
        NerdIconOption("Compras / Mercado", "Casa", NerdIcons.Cart, listOf("supermercado", "lista", "compras")),
        NerdIconOption("Sacola / Compras", "Casa", NerdIcons.Shopping, listOf("loja", "mercado")),
        NerdIconOption("Caixa / Encomenda", "Casa", NerdIcons.Package, listOf("correio", "entrega", "organizar")),
        NerdIconOption("Caixa de Correio", "Casa", NerdIcons.Mailbox, listOf("cartas", "correspondencia")),
        NerdIconOption("Ferramentas", "Casa", NerdIcons.Tools, listOf("conserto", "reparo", "bricolagem")),
        NerdIconOption("Chave Inglesa / Manutenção", "Casa", NerdIcons.Wrench, listOf("consertar", "manutencao")),
        NerdIconOption("Martelo", "Casa", NerdIcons.Hammer, listOf("obra", "reparo", "pregar")),
        NerdIconOption("Pá / Jardim", "Casa", NerdIcons.Shovel, listOf("jardinagem", "terra", "quintal")),
        NerdIconOption("Regador / Regar", "Casa", NerdIcons.WateringCan, listOf("plantas", "jardim", "regar")),
        NerdIconOption("Flor", "Casa", NerdIcons.Flower, listOf("planta", "jardim", "natureza")),
        NerdIconOption("Planta em Vaso", "Casa", NerdIcons.Plant, listOf("vaso", "regar", "jardinagem")),
        NerdIconOption("Vaso Sanitário / Banheiro", "Casa", NerdIcons.Toilet, listOf("banheiro", "limpeza")),
        NerdIconOption("Cachorro / Pet", "Casa", NerdIcons.Dog, listOf("passear", "cao", "racao", "animal")),
        NerdIconOption("Gato / Pet", "Casa", NerdIcons.Cat, listOf("felino", "racao", "areia", "animal")),
        NerdIconOption("Pata / Animal", "Casa", NerdIcons.Paw, listOf("pet", "veterinario", "animal")),
        NerdIconOption("Peixe / Aquário", "Casa", NerdIcons.Fish, listOf("aquario", "alimentar", "omega")),
        NerdIconOption("Bebê / Carrinho", "Casa", NerdIcons.BabyCarriage, listOf("filho", "crianca", "familia")),
        NerdIconOption("Carro", "Casa", NerdIcons.Car, listOf("dirigir", "transito", "lavar carro")),
        NerdIconOption("Moto", "Casa", NerdIcons.Motorbike, listOf("motocicleta", "transporte")),
        NerdIconOption("Ônibus", "Casa", NerdIcons.Bus, listOf("transporte", "viagem")),

        // Saúde, Higiene & Alimentação
        NerdIconOption("Copo de Água", "Saúde", NerdIcons.CupWater, listOf("hidratacao", "beber agua", "liquido")),
        NerdIconOption("Gota", "Saúde", NerdIcons.Water, listOf("agua", "hidratacao", "colirio")),
        NerdIconOption("Garrafa / Squeeze", "Saúde", NerdIcons.BottleTonic, listOf("agua", "suplemento", "garrafa")),
        NerdIconOption("Coração", "Saúde", NerdIcons.Heart, listOf("amor", "cardio", "saude")),
        NerdIconOption("Batimentos Cardíacos", "Saúde", NerdIcons.HeartPulse, listOf("pressao", "cardio", "exame")),
        NerdIconOption("Estetoscópio / Médico", "Saúde", NerdIcons.Stethoscope, listOf("consulta", "medico", "exame")),
        NerdIconOption("Médico", "Saúde", NerdIcons.Doctor, listOf("consulta", "clinica", "saude")),
        NerdIconOption("Hospital", "Saúde", NerdIcons.Hospital, listOf("exame", "clinica", "emergencia")),
        NerdIconOption("Remédio / Cápsula", "Saúde", NerdIcons.Pill, listOf("medicamento", "vitamina", "suplemento", "comprimido")),
        NerdIconOption("Vacina / Seringa", "Saúde", NerdIcons.Needle, listOf("injecao", "insulina", "exame")),
        NerdIconOption("Termômetro", "Saúde", NerdIcons.Thermometer, listOf("temperatura", "febre", "saude")),
        NerdIconOption("Máscara", "Saúde", NerdIcons.FaceMask, listOf("protecao", "skincare", "saude")),
        NerdIconOption("Lavar as Mãos", "Saúde", NerdIcons.HandWash, listOf("higiene", "sabonete", "limpeza")),
        NerdIconOption("Creme / Skincare", "Saúde", NerdIcons.Lotion, listOf("protetor solar", "hidratante", "pele", "rosto")),
        NerdIconOption("Batom / Maquiagem", "Saúde", NerdIcons.Lipstick, listOf("beleza", "autocuidado", "rotina")),
        NerdIconOption("Unhas / Manicure", "Saúde", NerdIcons.Nail, listOf("beleza", "cuidado", "maos")),
        NerdIconOption("Banho / Chuveiro", "Saúde", NerdIcons.Shower, listOf("ducha", "higiene", "tomar banho")),
        NerdIconOption("Banheira / Relaxar", "Saúde", NerdIcons.Bath, listOf("banho", "spa", "descanso")),
        NerdIconOption("Balança", "Saúde", NerdIcons.ScaleBathroom, listOf("peso", "pesagem", "dieta")),
        NerdIconOption("Dente", "Saúde", NerdIcons.Tooth, listOf("dentista", "fio dental", "boca")),
        NerdIconOption("Escovar os Dentes", "Saúde", NerdIcons.Toothbrush, listOf("higiene bucal", "escova", "pasta")),
        NerdIconOption("Olho / Visão", "Saúde", NerdIcons.Eye, listOf("colirio", "descansar vista", "oftalmologista")),
        NerdIconOption("Ouvido / Audição", "Saúde", NerdIcons.EarHearing, listOf("escutar", "otorrino")),
        NerdIconOption("Cama / Dormir", "Saúde", NerdIcons.Bed, listOf("sono", "deitar", "arrumar cama")),
        NerdIconOption("Sono / Zzz", "Saúde", NerdIcons.Sleep, listOf("descansar", "soneca", "dormir cedo")),
        NerdIconOption("Maçã / Fruta", "Saúde", NerdIcons.FoodApple, listOf("fruta", "dieta", "lanche", "saudavel")),
        NerdIconOption("Nutrição / Salada", "Saúde", NerdIcons.Nutrition, listOf("dieta", "verdura", "vegetal")),
        NerdIconOption("Cenoura", "Saúde", NerdIcons.Carrot, listOf("legume", "vegetal", "dieta")),
        NerdIconOption("Cerejas", "Saúde", NerdIcons.FruitCherries, listOf("fruta", "sobremesa")),
        NerdIconOption("Melancia", "Saúde", NerdIcons.FruitWatermelon, listOf("fruta", "verao")),
        NerdIconOption("Tigela / Refeição", "Saúde", NerdIcons.BowlMix, listOf("aveia", "cereal", "salada", "jantar")),
        NerdIconOption("Arroz / Marmita", "Saúde", NerdIcons.Rice, listOf("almoco", "refeicao", "marmita")),
        NerdIconOption("Macarrão", "Saúde", NerdIcons.Noodles, listOf("massa", "refeicao")),
        NerdIconOption("Pão / Café da Manhã", "Saúde", NerdIcons.BreadSlice, listOf("lanche", "torrada", "cafe da manha")),
        NerdIconOption("Refeição / Fast", "Saúde", NerdIcons.Food, listOf("comida", "lanche")),
        NerdIconOption("Café", "Saúde", NerdIcons.Coffee, listOf("cafeina", "xicara", "pausa")),
        NerdIconOption("Chá", "Saúde", NerdIcons.Tea, listOf("cha", "camomila", "infusao")),
        NerdIconOption("Xícara", "Saúde", NerdIcons.Cup, listOf("cha", "cafe", "bebida")),
        NerdIconOption("Caneca", "Saúde", NerdIcons.Mug, listOf("bebida", "cafe", "cha")),
        NerdIconOption("Taça / Vinho", "Saúde", NerdIcons.Wine, listOf("alcool", "festa", "sem alcool")),
        NerdIconOption("Cerveja / Bebida", "Saúde", NerdIcons.Beer, listOf("bar", "sem alcool", "social")),
        NerdIconOption("Bandeja / Serviço", "Saúde", NerdIcons.RoomService, listOf("refeicao", "almoco", "janta")),
        NerdIconOption("Bolo / Doce", "Saúde", NerdIcons.Cake, listOf("aniversario", "sobremesa", "acucar")),
        NerdIconOption("Bala / Doce", "Saúde", NerdIcons.Candy, listOf("acucar", "sem doce", "lanche")),
        NerdIconOption("Óleo / Azeite", "Saúde", NerdIcons.Oil, listOf("cozinha", "omega", "tempero")),
        NerdIconOption("Biscoito", "Saúde", NerdIcons.Cookie, listOf("lanche", "doce")),
        NerdIconOption("Sorvete", "Saúde", NerdIcons.IceCream, listOf("doce", "sobremesa")),
        NerdIconOption("Pizza", "Saúde", NerdIcons.Pizza, listOf("comida", "fim de semana")),
        NerdIconOption("Cuidado / Carinho", "Saúde", NerdIcons.HandHeart, listOf("autocuidado", "caridade", "ajuda")),

        // Esportes & Movimento
        NerdIconOption("Halter / Academia", "Esportes", NerdIcons.Dumbbell, listOf("treino", "musculacao", "peso")),
        NerdIconOption("Levantamento de Peso", "Esportes", NerdIcons.WeightLifter, listOf("academia", "crossfit", "forca")),
        NerdIconOption("Braço Forte", "Esportes", NerdIcons.ArmFlex, listOf("musculo", "treino", "forca")),
        NerdIconOption("Kettlebell / Peso", "Esportes", NerdIcons.Weight, listOf("exercicio", "forca")),
        NerdIconOption("Corrida", "Esportes", NerdIcons.Run, listOf("correr", "cardio", "maratona")),
        NerdIconOption("Caminhada", "Esportes", NerdIcons.Walk, listOf("andar", "passos", "passeio")),
        NerdIconOption("Trilha / Montanha", "Esportes", NerdIcons.Hiking, listOf("caminhada", "natureza", "escalada")),
        NerdIconOption("Bicicleta / Pedal", "Esportes", NerdIcons.Bike, listOf("ciclismo", "bike", "pedalar")),
        NerdIconOption("Tênis / Calçado", "Esportes", NerdIcons.ShoeSneaker, listOf("corrida", "passos", "caminhada")),
        NerdIconOption("Pular Corda", "Esportes", NerdIcons.JumpRope, listOf("cardio", "treino", "corda")),
        NerdIconOption("Escada / Subir", "Esportes", NerdIcons.Stairs, listOf("degraus", "cardio")),
        NerdIconOption("Yoga", "Esportes", NerdIcons.Yoga, listOf("alongamento", "postura", "pilates")),
        NerdIconOption("Natação / Piscina", "Esportes", NerdIcons.Swim, listOf("nadar", "agua", "esporte")),
        NerdIconOption("Boxe / Luta", "Esportes", NerdIcons.BoxingGlove, listOf("luta", "muay thai", "cardio")),
        NerdIconOption("Karatê / Artes Marciais", "Esportes", NerdIcons.Karate, listOf("jiu jitsu", "luta", "faixa")),
        NerdIconOption("Futebol", "Esportes", NerdIcons.Soccer, listOf("bola", "pelada", "jogo")),
        NerdIconOption("Basquete", "Esportes", NerdIcons.Basketball, listOf("bola", "quadra")),
        NerdIconOption("Futebol Americano", "Esportes", NerdIcons.Football, listOf("bola", "esporte")),
        NerdIconOption("Tênis / Raquete", "Esportes", NerdIcons.Tennis, listOf("beach tennis", "padel", "esporte")),
        NerdIconOption("Golfe", "Esportes", NerdIcons.Golf, listOf("esporte", "campo")),
        NerdIconOption("Skate", "Esportes", NerdIcons.Skate, listOf("patins", "rua")),
        NerdIconOption("Esqui", "Esportes", NerdIcons.Ski, listOf("neve", "inverno")),

        // Mente, Espiritualidade & Energia
        NerdIconOption("Meditação", "Mente", NerdIcons.Meditation, listOf("mindfulness", "respirar", "calma", "zen")),
        NerdIconOption("Cérebro / Foco", "Mente", NerdIcons.Brain, listOf("concentracao", "memoria", "mente", "psicologia")),
        NerdIconOption("Oração / Gratidão", "Mente", NerdIcons.Prayer, listOf("rezar", "espiritualidade", "agradecer")),
        NerdIconOption("Vela / Relaxar", "Mente", NerdIcons.Candle, listOf("calma", "ritual", "aroma")),
        NerdIconOption("Fogueira / Acampar", "Mente", NerdIcons.Campfire, listOf("fogo", "natureza", "calma")),
        NerdIconOption("Folha / Natureza", "Mente", NerdIcons.Leaf, listOf("ecologia", "natural", "vegano")),
        NerdIconOption("Broto / Crescimento", "Mente", NerdIcons.Sprout, listOf("evolucao", "novo habito", "semente")),
        NerdIconOption("Árvore", "Mente", NerdIcons.Tree, listOf("parque", "natureza", "ar livre")),
        NerdIconOption("Fogo / Sequência", "Mente", NerdIcons.Fire, listOf("streak", "foco", "energia")),
        NerdIconOption("Raio / Energia", "Mente", NerdIcons.Flash, listOf("disposicao", "rapido", "energia")),
        NerdIconOption("Bateria Carregando", "Mente", NerdIcons.BatteryCharging, listOf("descanso", "recarregar", "celular")),
        NerdIconOption("Lâmpada / Ideia", "Mente", NerdIcons.Lightbulb, listOf("criatividade", "insight", "pensar")),
        NerdIconOption("Sol / Manhã", "Mente", NerdIcons.Sun, listOf("dia", "luz solar", "vitamina d", "acordar")),
        NerdIconOption("Lua / Noite", "Mente", NerdIcons.Moon, listOf("noite", "rotina noturna", "dormir")),
        NerdIconOption("Rosto Feliz / Humor", "Mente", NerdIcons.EmoticonHappy, listOf("diario", "humor", "alegria")),
        NerdIconOption("Ampulheta / Pausa", "Mente", NerdIcons.Hourglass, listOf("pomodoro", "tempo", "esperar")),

        // Estudos & Trabalho
        NerdIconOption("Livro Aberto / Leitura", "Estudos", NerdIcons.BookOpen, listOf("ler", "estudar", "paginas")),
        NerdIconOption("Livro Fechado", "Estudos", NerdIcons.Book, listOf("leitura", "biblioteca", "curso")),
        NerdIconOption("Bíblia / Livro Sagrado", "Estudos", NerdIcons.Bible, listOf("leitura", "fe", "estudo")),
        NerdIconOption("Caderno / Diário", "Estudos", NerdIcons.Notebook, listOf("anotar", "journal", "resumo")),
        NerdIconOption("Documento / Texto", "Estudos", NerdIcons.FileDocument, listOf("relatorio", "escrever", "pdf")),
        NerdIconOption("Jornal / Notícias", "Estudos", NerdIcons.Newspaper, listOf("informacao", "ler", "artigo")),
        NerdIconOption("Chapéu / Faculdade", "Estudos", NerdIcons.School, listOf("aula", "curso", "prova", "estudo")),
        NerdIconOption("Átomo / Ciência", "Estudos", NerdIcons.Atom, listOf("fisica", "quimica", "estudo")),
        NerdIconOption("Idiomas / Tradução", "Estudos", NerdIcons.Language, listOf("ingles", "espanhol", "lingua")),
        NerdIconOption("Notebook / PC", "Estudos", NerdIcons.Laptop, listOf("computador", "trabalho", "programar")),
        NerdIconOption("Monitor / Desktop", "Estudos", NerdIcons.Monitor, listOf("tela", "trabalho", "escritorio")),
        NerdIconOption("Teclado", "Estudos", NerdIcons.Keyboard, listOf("digitar", "codigo", "escrever")),
        NerdIconOption("Mesa de Escritório", "Estudos", NerdIcons.Desk, listOf("home office", "organizar mesa")),
        NerdIconOption("Maleta / Trabalho", "Estudos", NerdIcons.Briefcase, listOf("emprego", "reuniao", "negocios")),
        NerdIconOption("Código / Programação", "Estudos", NerdIcons.CodeTags, listOf("dev", "software", "ti", "github")),
        NerdIconOption("Robô / IA", "Estudos", NerdIcons.Robot, listOf("automacao", "tecnologia", "ia")),
        NerdIconOption("Lápis / Escrever", "Estudos", NerdIcons.Pencil, listOf("desenho", "escrever", "editar")),
        NerdIconOption("Caneta Tinteiro", "Estudos", NerdIcons.FountainPen, listOf("caligrafia", "diario", "assinar")),
        NerdIconOption("Régua / Projeto", "Estudos", NerdIcons.Ruler, listOf("medir", "design", "arquitetura")),
        NerdIconOption("Clipe / Anexo", "Estudos", NerdIcons.Paperclip, listOf("arquivo", "documento")),
        NerdIconOption("Pasta / Organização", "Estudos", NerdIcons.Folder, listOf("arquivos", "organizar")),
        NerdIconOption("Arquivo / Caixa", "Estudos", NerdIcons.Archive, listOf("guardar", "limpar email")),
        NerdIconOption("Prancheta / Checklist", "Estudos", NerdIcons.ClipboardCheck, listOf("revisao", "tarefas", "auditoria")),
        NerdIconOption("E-mail / Caixa de Entrada", "Estudos", NerdIcons.Email, listOf("responder", "mensagens", "inbox")),
        NerdIconOption("Chat / Mensagem", "Estudos", NerdIcons.Chat, listOf("responder", "familia", "amigos")),
        NerdIconOption("Telefone / Ligar", "Estudos", NerdIcons.Call, listOf("ligacao", "contato")),
        NerdIconOption("Impressora", "Estudos", NerdIcons.Printer, listOf("imprimir", "papel")),
        NerdIconOption("Mochila", "Estudos", NerdIcons.BagPersonal, listOf("escola", "viagem", "arrumar")),

        // Finanças & Planejamento
        NerdIconOption("Dinheiro / Notas", "Finanças", NerdIcons.Cash, listOf("salario", "pagar", "orcamento")),
        NerdIconOption("Cofrinho / Poupança", "Finanças", NerdIcons.PiggyBank, listOf("guardar", "economizar", "investir")),
        NerdIconOption("Banco", "Finanças", NerdIcons.Bank, listOf("conta", "transferencia", "financas")),
        NerdIconOption("Cartão de Crédito", "Finanças", NerdIcons.CreditCard, listOf("fatura", "gastos", "compras")),
        NerdIconOption("Carteira", "Finanças", NerdIcons.Wallet, listOf("gastos", "dinheiro", "controle")),
        NerdIconOption("Calculadora", "Finanças", NerdIcons.Calculator, listOf("contas", "imposto", "planilha")),

        // Lazer, Arte & Viagem
        NerdIconOption("Música / Fone", "Lazer", NerdIcons.Music, listOf("playlist", "tocar", "audio")),
        NerdIconOption("Nota Musical", "Lazer", NerdIcons.MusicNote, listOf("canto", "instrumento", "musica")),
        NerdIconOption("Fones de Ouvido", "Lazer", NerdIcons.Headphones, listOf("podcast", "audiobook", "musica")),
        NerdIconOption("Violão / Guitarra", "Lazer", NerdIcons.GuitarAcoustic, listOf("tocar", "praticar", "instrumento")),
        NerdIconOption("Piano / Teclado", "Lazer", NerdIcons.Piano, listOf("tocar", "musica")),
        NerdIconOption("Violino", "Lazer", NerdIcons.Violin, listOf("instrumento", "musica")),
        NerdIconOption("Caixa de Som", "Lazer", NerdIcons.Speaker, listOf("audio", "musica")),
        NerdIconOption("Rádio", "Lazer", NerdIcons.Radio, listOf("podcast", "noticias", "musica")),
        NerdIconOption("Microfone", "Lazer", NerdIcons.Microphone, listOf("voz", "gravar", "podcast")),
        NerdIconOption("Podcast", "Lazer", NerdIcons.Podcast, listOf("ouvir", "episodio")),
        NerdIconOption("Paleta / Pintura", "Lazer", NerdIcons.Palette, listOf("arte", "desenhar", "cor")),
        NerdIconOption("Pincel", "Lazer", NerdIcons.Paintbrush, listOf("pintar", "arte")),
        NerdIconOption("Desenhar / Caneta", "Lazer", NerdIcons.Draw, listOf("ilustracao", "esboco", "arte")),
        NerdIconOption("Pena / Escrita", "Lazer", NerdIcons.Feather, listOf("poesia", "escrever", "leve")),
        NerdIconOption("Tesoura / Artesanato", "Lazer", NerdIcons.Scissors, listOf("cortar", "papel", "cabelo")),
        NerdIconOption("Origami", "Lazer", NerdIcons.Origami, listOf("dobradura", "hobby")),
        NerdIconOption("Câmera / Foto", "Lazer", NerdIcons.Camera, listOf("fotografia", "registro")),
        NerdIconOption("Imagem / Galeria", "Lazer", NerdIcons.Image, listOf("fotos", "organizar fotos")),
        NerdIconOption("Vídeo / Gravar", "Lazer", NerdIcons.Video, listOf("youtube", "editar", "camera")),
        NerdIconOption("Claquete / Cinema", "Lazer", NerdIcons.Movie, listOf("filme", "serie", "gravar")),
        NerdIconOption("Filme / Cinema", "Lazer", NerdIcons.Film, listOf("assistir", "serie")),
        NerdIconOption("TV / Série", "Lazer", NerdIcons.Tv, listOf("televisao", "tempo de tela")),
        NerdIconOption("Ingresso / Evento", "Lazer", NerdIcons.Ticket, listOf("show", "teatro", "cinema")),
        NerdIconOption("Controle / Games", "Lazer", NerdIcons.Controller, listOf("videogame", "jogar", "lazer")),
        NerdIconOption("Baralho / Cartas", "Lazer", NerdIcons.Cards, listOf("jogo", "flashcards", "estudo")),
        NerdIconOption("Sinuca / Bilhar", "Lazer", NerdIcons.Pool, listOf("jogo", "lazer")),
        NerdIconOption("Xadrez", "Lazer", NerdIcons.Chess, listOf("jogo", "estrategia")),
        NerdIconOption("Dado / RPG", "Lazer", NerdIcons.Dice, listOf("jogos", "sorte")),
        NerdIconOption("Quebra-cabeça", "Lazer", NerdIcons.Puzzle, listOf("logica", "hobby", "resolver")),
        NerdIconOption("Telescópio", "Lazer", NerdIcons.Telescope, listOf("astronomia", "ceu", "observar")),
        NerdIconOption("Binóculos", "Lazer", NerdIcons.Binoculars, listOf("observar", "natureza", "trilha")),
        NerdIconOption("Avião / Viagem", "Lazer", NerdIcons.Airplane, listOf("voo", "ferias", "mala")),
        NerdIconOption("Viagem / Embarque", "Lazer", NerdIcons.Plane, listOf("aviao", "passagem", "ferias")),
        NerdIconOption("Praia / Férias", "Lazer", NerdIcons.Beach, listOf("sol", "mar", "descanso")),
        NerdIconOption("Barraca / Acampamento", "Lazer", NerdIcons.Tent, listOf("camping", "natureza")),
        NerdIconOption("Bússola / Explorar", "Lazer", NerdIcons.Compass, listOf("aventura", "direcao")),
        NerdIconOption("Mapa / Rota", "Lazer", NerdIcons.Map, listOf("passeio", "viagem")),
        NerdIconOption("Estrada / Viagem", "Lazer", NerdIcons.Road, listOf("dirigir", "trajeto", "viagem")),
        NerdIconOption("Veleiro / Barco", "Lazer", NerdIcons.Sailing, listOf("mar", "lago")),
        NerdIconOption("Trem / Metrô", "Lazer", NerdIcons.Train, listOf("viagem", "transporte")),

        // Geral & Metas
        NerdIconOption("Alvo / Meta", "Geral", NerdIcons.Target, listOf("objetivo", "foco", "meta")),
        NerdIconOption("Troféu / Conquista", "Geral", NerdIcons.Trophy, listOf("premio", "vitoria", "recorde")),
        NerdIconOption("Coroa / Destaque", "Geral", NerdIcons.Crown, listOf("rei", "top", "lider")),
        NerdIconOption("Diamante / Valor", "Geral", NerdIcons.Diamond, listOf("precioso", "qualidade")),
        NerdIconOption("Estrela / Favorito", "Geral", NerdIcons.Star, listOf("importante", "destaque")),
        NerdIconOption("Bandeira / Marco", "Geral", NerdIcons.Flag, listOf("marco", "meta", "etapa")),
        NerdIconOption("Marcador / Salvo", "Geral", NerdIcons.Bookmark, listOf("lembrar", "favorito")),
        NerdIconOption("Tag / Rótulo", "Geral", NerdIcons.Tag, listOf("categoria", "etiqueta")),
        NerdIconOption("Foguete / Lançamento", "Geral", NerdIcons.RocketLaunch, listOf("projeto", "startup", "avancar")),
        NerdIconOption("Presente", "Geral", NerdIcons.Gift, listOf("aniversario", "surpresa", "recompensa")),
        NerdIconOption("Relógio Rápido", "Geral", NerdIcons.ClockFast, listOf("urgente", "prazo", "rapido")),
        NerdIconOption("Cronômetro", "Geral", NerdIcons.Timer, listOf("tempo", "pomodoro", "treino")),
        NerdIconOption("Relógio de Pulso", "Geral", NerdIcons.Watch, listOf("pontualidade", "tempo")),
        NerdIconOption("Escudo / Proteção", "Geral", NerdIcons.Shield, listOf("seguranca", "backup")),
        NerdIconOption("Celular / Sem Tela", "Geral", NerdIcons.Cellphone, listOf("detox digital", "telefone")),
        NerdIconOption("Wi-Fi / Internet", "Geral", NerdIcons.Wifi, listOf("rede", "desconectar")),
        NerdIconOption("Planeta / Mundo", "Geral", NerdIcons.Earth, listOf("global", "ambiente")),
        NerdIconOption("Nuvem / Clima", "Geral", NerdIcons.Cloud, listOf("tempo", "backup", "ceu")),
        NerdIconOption("Chuva", "Geral", NerdIcons.WeatherRain, listOf("clima", "guarda-chuva", "agua")),
        NerdIconOption("Neve / Frio", "Geral", NerdIcons.WeatherSnow, listOf("inverno", "frio")),
        NerdIconOption("Guarda-chuva / Clima", "Geral", NerdIcons.Umbrella, listOf("chuva", "protecao")),
        NerdIconOption("Acessibilidade", "Geral", NerdIcons.Wheelchair, listOf("mobilidade", "saude")),
        NerdIconOption("Óculos", "Geral", NerdIcons.Glasses, listOf("leitura", "vista")),
        NerdIconOption("Igreja / Fé", "Geral", NerdIcons.Church, listOf("culto", "missa", "espiritualidade")),
        NerdIconOption("Cidade / Urbano", "Geral", NerdIcons.City, listOf("rua", "trabalho", "centro"))
    )

    val categories: List<String> = all.map { it.category }.distinct()
}

fun momentNerdGlyph(startMinutesOfDay: Int?): String = when (startMinutesOfDay) {
    null -> NerdIcons.ClockOutline
    in 300 until 720 -> NerdIcons.Sun
    in 720 until 1080 -> NerdIcons.WeatherPartlyCloudy
    in 1080 until 1320 -> NerdIcons.Sunset
    else -> NerdIcons.Sleep
}

@Suppress("DEPRECATION")
@Composable
fun NerdIcon(
    glyph: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    size: TextUnit = 18.sp
) {
    Text(
        text = glyph,
        modifier = modifier,
        color = color,
        style = TextStyle(
            fontFamily = UbuntuSansNerdFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = size,
            lineHeight = size,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
    )
}
