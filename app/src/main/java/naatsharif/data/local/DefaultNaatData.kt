package naatsharif.data.local

object DefaultNaatData {
    val defaultIdsToRemove = listOf(
        "naat_faslon_ko",
        "naat_tu_kuja",
        "naat_bhar_do_jholi",
        "naat_karam_mangta_hoon",
        "hamd_woh_wohi_khuda",
        "hamd_allahu_allahu",
        "raza_mustafa_jane_rehmat",
        "raza_chamak_tujhse_paate",
        "gouse_baghdad_wale_dulha",
        "gareeb_khwaja_mere_khwaja",
        "manqabat_ali_maula",
        "panjtan_hussain_ibne_ali",
        "salami_ya_nabi_salam_alaika",
        "naat_1", "naat_2", "naat_3", "naat_4", "naat_5",
        "naat_6", "naat_7", "naat_8", "naat_9", "naat_10",
        "default_1", "default_2", "default_3", "default_4"
    )

    val defaultTitleSnippetsToRemove = listOf(
        "फासलों को तकल्लुफ",
        "तू कुजा मन कुजा",
        "भर दो झोली",
        "करम मांगता हूँ",
        "कोई तो है जो निज़ामे हस्ती",
        "अल्लाहु अल्लाहु",
        "मुस्तफा जाने रहमत",
        "चमक तुझसे पाते",
        "बगदाद वाले दूल्हा",
        "ख्वाजा मेरे ख्वाजा",
        "अली मौला अली मौला",
        "हुसैन इब्ने अली",
        "या नबी सलाम अलैका",
        "faslon ko takalluf",
        "tu kuja man kuja",
        "bhar do jholi",
        "karam mangta hoon",
        "woh wohi khuda hai",
        "allahu allahu",
        "mustafa jane rehmat",
        "chamak tujhse paate",
        "baghdad wale dulha",
        "khwaja mere khwaja",
        "ali maula",
        "hussain ibne ali",
        "ya nabi salam alaika"
    )

    fun isDefaultNaat(id: String, titleHindi: String, titleEnglish: String): Boolean {
        if (defaultIdsToRemove.contains(id)) return true
        
        val titleH = titleHindi.lowercase()
        val titleE = titleEnglish.lowercase()
        for (snippet in defaultTitleSnippetsToRemove) {
            val s = snippet.lowercase()
            if (titleH.contains(s) || titleE.contains(s)) return true
        }
        return false
    }

    val initialNaats = listOf<NaatEntity>()
}
