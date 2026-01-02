package com.example.museapp.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.museapp.domain.model.Country
import com.example.museapp.presentation.ui.components.appTextFieldColors
import com.example.museapp.ui.theme.AppTypography
import com.example.museapp.ui.theme.PrimaryColor
import com.example.museapp.util.getAllCountries

@Composable
fun CountryPickerScreen(
    onBack: () -> Unit,
    onCountrySelected: (Country) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val countries = remember { getAllCountries() }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val filtered = remember(search, countries) {
        if (search.isBlank()) countries
        else countries.filter { it.name.contains(search, ignoreCase = true) }
    }

    Surface(
        modifier = Modifier.fillMaxSize().padding(top = topInset),
        color = Color.White,


    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = {
                        Text(
                            "Search by country name...",
                            style = AppTypography.titleSmall
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    colors = appTextFieldColors(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            Divider(Modifier.padding(bottom = 2.dp) , color = PrimaryColor)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered) { country ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCountrySelected(country) }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .heightIn(min = 38.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flag container with fixed size so all flags align
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = country.flagEmoji,
                                style = AppTypography.titleSmall,
                                fontSize = 20.sp
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        // Country name takes remaining space and keeps single line
                        Text(
                            text = country.name,
                            style = AppTypography.bodyLarge,
                            modifier = Modifier
                                .weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Dial code aligned to the right because name uses weight
                        Text(
                            text = country.code,
                            style = AppTypography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Divider(color = PrimaryColor)
                }
            }
        }
    }
}
