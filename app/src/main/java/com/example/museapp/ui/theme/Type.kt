package com.example.museapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// App-wide FontFamily pointing to res/font/app_font_regular.ttf
//val AppFontFamily = FontFamily(
//    Font(R.font.poppins, FontWeight.Normal)
//)

// Set of Material typography styles with app font
val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // you can add/override more text styles as needed (labelSmall, titleMedium, etc.)
)
