package com.example.lokkala.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lokkala.domain.model.Country
import com.example.lokkala.util.getAllCountries

@Composable
fun CountryPickerScreen(
    onBack: () -> Unit,
    onCountrySelected: (Country) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val countries = remember { getAllCountries() }

    val filtered = remember(search, countries) {
        if (search.isBlank()) countries
        else countries.filter { it.name.contains(search, ignoreCase = true) }
    }

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
                placeholder = { Text("Search by country name...") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        }

        Divider(Modifier.padding(bottom = 4.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered) { country ->
                Row(
                    Modifier
                        .weight(2f).fillMaxWidth()
                        .clickable { onCountrySelected(country) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        country.flagEmoji,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        country.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = country.code, // Reverted to show complete code
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}