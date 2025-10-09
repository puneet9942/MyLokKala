package com.example.lokkala.util

import android.util.Patterns

import com.google.i18n.phonenumbers.PhoneNumberUtil

object ValidationUtils {

    /**
     * Dynamically map country calling code to ISO code using libphonenumber.
     * Example: "+91" -> "IN", "+93" -> "AF"
     */
    fun getCountryIsoFromCode(countryCode: String): String? {
        val code = countryCode.replace("+", "").trimStart('0')
        return try {
            PhoneNumberUtil.getInstance().getRegionCodesForCountryCode(code.toInt()).firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if the given phone number is valid for the given country using libphonenumber.
     * @param phone The phone number including country code, e.g. "+919876543210"
     * @param countryIso The ISO code, e.g. "IN" for India, "US" for USA.
     */
    fun isValidPhoneForCountry(phone: String, countryIso: String): Boolean {
        val util = PhoneNumberUtil.getInstance()
        return try {
            val number = util.parse(phone, countryIso)
            util.isValidNumber(number)
        } catch (e: Exception) {
            false
        }
    }



    /**
     * Helper to map country calling code to ISO country code.
     * Extend this with more countries as needed.
     */
    /*fun getCountryIsoFromCode(countryCode: String): String? = when (countryCode) {
        "+93" -> "AF"   // Afghanistan
        "+355" -> "AL"  // Albania
        "+213" -> "DZ"  // Algeria
        "+376" -> "AD"  // Andorra
        "+244" -> "AO"  // Angola
        "+1264" -> "AI" // Anguilla
        "+1268" -> "AG" // Antigua and Barbuda
        "+54" -> "AR"   // Argentina
        "+374" -> "AM"  // Armenia
        "+297" -> "AW"  // Aruba
        "+61" -> "AU"   // Australia
        "+43" -> "AT"   // Austria
        "+994" -> "AZ"  // Azerbaijan
        "+1242" -> "BS" // Bahamas
        "+973" -> "BH"  // Bahrain
        "+880" -> "BD"  // Bangladesh
        "+1246" -> "BB" // Barbados
        "+375" -> "BY"  // Belarus
        "+32" -> "BE"   // Belgium
        "+501" -> "BZ"  // Belize
        "+229" -> "BJ"  // Benin
        "+1441" -> "BM" // Bermuda
        "+975" -> "BT"  // Bhutan
        "+591" -> "BO"  // Bolivia
        "+387" -> "BA"  // Bosnia and Herzegovina
        "+267" -> "BW"  // Botswana
        "+55" -> "BR"   // Brazil
        "+246" -> "IO"  // British Indian Ocean Territory
        "+673" -> "BN"  // Brunei Darussalam
        "+359" -> "BG"  // Bulgaria
        "+226" -> "BF"  // Burkina Faso
        "+257" -> "BI"  // Burundi
        "+855" -> "KH"  // Cambodia
        "+237" -> "CM"  // Cameroon
        "+1" -> "US"    // United States/Canada
        "+238" -> "CV"  // Cape Verde
        "+1345" -> "KY" // Cayman Islands
        "+236" -> "CF"  // Central African Republic
        "+235" -> "TD"  // Chad
        "+56" -> "CL"   // Chile
        "+86" -> "CN"   // China
        "+57" -> "CO"   // Colombia
        "+269" -> "KM"  // Comoros
        "+242" -> "CG"  // Congo
        "+243" -> "CD"  // Congo, The Democratic Republic
        "+682" -> "CK"  // Cook Islands
        "+506" -> "CR"  // Costa Rica
        "+225" -> "CI"  // Côte d’Ivoire
        "+385" -> "HR"  // Croatia
        "+53" -> "CU"   // Cuba
        "+357" -> "CY"  // Cyprus
        "+420" -> "CZ"  // Czech Republic
        "+45" -> "DK"   // Denmark
        "+253" -> "DJ"  // Djibouti
        "+1767" -> "DM" // Dominica
        "+1" -> "CA"    // Canada (already included as US)
        "+1809", "+1829", "+1849" -> "DO" // Dominican Republic (multiple codes)
        "+593" -> "EC"  // Ecuador
        "+20" -> "EG"   // Egypt
        "+503" -> "SV"  // El Salvador
        "+240" -> "GQ"  // Equatorial Guinea
        "+291" -> "ER"  // Eritrea
        "+372" -> "EE"  // Estonia
        "+251" -> "ET"  // Ethiopia
        "+500" -> "FK"  // Falkland Islands (Malvinas)
        "+298" -> "FO"  // Faroe Islands
        "+679" -> "FJ"  // Fiji
        "+358" -> "FI"  // Finland
        "+33" -> "FR"   // France
        "+594" -> "GF"  // French Guiana
        "+689" -> "PF"  // French Polynesia
        "+241" -> "GA"  // Gabon
        "+220" -> "GM"  // Gambia
        "+995" -> "GE"  // Georgia
        "+49" -> "DE"   // Germany
        "+233" -> "GH"  // Ghana
        "+350" -> "GI"  // Gibraltar
        "+30" -> "GR"   // Greece
        "+299" -> "GL"  // Greenland
        "+1473" -> "GD" // Grenada
        "+590" -> "GP"  // Guadeloupe
        "+1671" -> "GU" // Guam
        "+502" -> "GT"  // Guatemala
        "+44" -> "GB"   // United Kingdom
        "+224" -> "GN"  // Guinea
        "+245" -> "GW"  // Guinea-Bissau
        "+592" -> "GY"  // Guyana
        "+509" -> "HT"  // Haiti
        "+504" -> "HN"  // Honduras
        "+852" -> "HK"  // Hong Kong
        "+36" -> "HU"   // Hungary
        "+354" -> "IS"  // Iceland
        "+91" -> "IN"   // India
        "+62" -> "ID"   // Indonesia
        "+98" -> "IR"   // Iran
        "+964" -> "IQ"  // Iraq
        "+353" -> "IE"  // Ireland
        "+972" -> "IL"  // Israel
        "+39" -> "IT"   // Italy
        "+1876" -> "JM" // Jamaica
        "+81" -> "JP"   // Japan
        "+962" -> "JO"  // Jordan
        "+7" -> "KZ"    // Kazakhstan
        "+254" -> "KE"  // Kenya
        "+686" -> "KI"  // Kiribati
        "+850" -> "KP"  // North Korea
        "+82" -> "KR"   // South Korea
        "+965" -> "KW"  // Kuwait
        "+996" -> "KG"  // Kyrgyzstan
        "+856" -> "LA"  // Laos
        "+371" -> "LV"  // Latvia
        "+961" -> "LB"  // Lebanon
        "+266" -> "LS"  // Lesotho
        "+231" -> "LR"  // Liberia
        "+218" -> "LY"  // Libya
        "+423" -> "LI"  // Liechtenstein
        "+370" -> "LT"  // Lithuania
        "+352" -> "LU"  // Luxembourg
        "+853" -> "MO"  // Macao
        "+389" -> "MK"  // Macedonia
        "+261" -> "MG"  // Madagascar
        "+265" -> "MW"  // Malawi
        "+60" -> "MY"   // Malaysia
        "+960" -> "MV"  // Maldives
        "+223" -> "ML"  // Mali
        "+356" -> "MT"  // Malta
        "+692" -> "MH"  // Marshall Islands
        "+596" -> "MQ"  // Martinique
        "+222" -> "MR"  // Mauritania
        "+230" -> "MU"  // Mauritius
        "+262" -> "YT"  // Mayotte
        "+52" -> "MX"   // Mexico
        "+691" -> "FM"  // Micronesia
        "+373" -> "MD"  // Moldova
        "+377" -> "MC"  // Monaco
        "+976" -> "MN"  // Mongolia
        "+382" -> "ME"  // Montenegro
        "+1664" -> "MS" // Montserrat
        "+212" -> "MA"  // Morocco
        "+258" -> "MZ"  // Mozambique
        "+95" -> "MM"   // Myanmar
        "+264" -> "NA"  // Namibia
        "+674" -> "NR"  // Nauru
        "+977" -> "NP"  // Nepal
        "+31" -> "NL"   // Netherlands
        "+599" -> "AN"  // Netherlands Antilles
        "+687" -> "NC"  // New Caledonia
        "+64" -> "NZ"   // New Zealand
        "+505" -> "NI"  // Nicaragua
        "+227" -> "NE"  // Niger
        "+234" -> "NG"  // Nigeria
        "+683" -> "NU"  // Niue
        "+672" -> "NF"  // Norfolk Island
        "+1670" -> "MP" // Northern Mariana Islands
        "+47" -> "NO"   // Norway
        "+968" -> "OM"  // Oman
        "+92" -> "PK"   // Pakistan
        "+680" -> "PW"  // Palau
        "+970" -> "PS"  // Palestine
        "+507" -> "PA"  // Panama
        "+675" -> "PG"  // Papua New Guinea
        "+595" -> "PY"  // Paraguay
        "+51" -> "PE"   // Peru
        "+63" -> "PH"   // Philippines
        "+48" -> "PL"   // Poland
        "+351" -> "PT"  // Portugal
        "+1787", "+1939" -> "PR" // Puerto Rico
        "+974" -> "QA"  // Qatar
        "+40" -> "RO"   // Romania
        "+7" -> "RU"    // Russia
        "+250" -> "RW"  // Rwanda
        "+290" -> "SH"  // Saint Helena
        "+1869" -> "KN" // Saint Kitts and Nevis
        "+1758" -> "LC" // Saint Lucia
        "+508" -> "PM"  // Saint Pierre and Miquelon
        "+1784" -> "VC" // Saint Vincent and the Grenadines
        "+685" -> "WS"  // Samoa
        "+378" -> "SM"  // San Marino
        "+239" -> "ST"  // Sao Tome and Principe
        "+966" -> "SA"  // Saudi Arabia
        "+221" -> "SN"  // Senegal
        "+381" -> "RS"  // Serbia
        "+248" -> "SC"  // Seychelles
        "+232" -> "SL"  // Sierra Leone
        "+65" -> "SG"   // Singapore
        "+421" -> "SK"  // Slovakia
        "+386" -> "SI"  // Slovenia
        "+677" -> "SB"  // Solomon Islands
        "+252" -> "SO"  // Somalia
        "+27" -> "ZA"   // South Africa
        "+211" -> "SS"  // South Sudan
        "+34" -> "ES"   // Spain
        "+94" -> "LK"   // Sri Lanka
        "+249" -> "SD"  // Sudan
        "+597" -> "SR"  // Suriname
        "+268" -> "SZ"  // Swaziland
        "+46" -> "SE"   // Sweden
        "+41" -> "CH"   // Switzerland
        "+963" -> "SY"  // Syrian Arab Republic
        "+886" -> "TW"  // Taiwan
        "+992" -> "TJ"  // Tajikistan
        "+255" -> "TZ"  // Tanzania
        "+66" -> "TH"   // Thailand
        "+228" -> "TG"  // Togo
        "+690" -> "TK"  // Tokelau
        "+676" -> "TO"  // Tonga
        "+1868" -> "TT" // Trinidad and Tobago
        "+216" -> "TN"  // Tunisia
        "+90" -> "TR"   // Turkey
        "+993" -> "TM"  // Turkmenistan
        "+1649" -> "TC" // Turks and Caicos Islands
        "+688" -> "TV"  // Tuvalu
        "+256" -> "UG"  // Uganda
        "+380" -> "UA"  // Ukraine
        "+971" -> "AE"  // United Arab Emirates
        "+44" -> "GB"   // United Kingdom
        "+1" -> "US"    // United States (again for safety)
        "+598" -> "UY"  // Uruguay
        "+998" -> "UZ"  // Uzbekistan
        "+678" -> "VU"  // Vanuatu
        "+58" -> "VE"   // Venezuela
        "+84" -> "VN"   // Vietnam
        "+1284" -> "VG" // Virgin Islands, British
        "+1340" -> "VI" // Virgin Islands, U.S.
        "+681" -> "WF"  // Wallis and Futuna
        "+967" -> "YE"  // Yemen
        "+260" -> "ZM"  // Zambia
        "+263" -> "ZW"  // Zimbabwe
        else -> null
    }*/

}