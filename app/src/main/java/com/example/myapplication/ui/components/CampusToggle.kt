package com.example.myapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.theme.ToggleWhite
import com.example.myapplication.ui.theme.lightMaroon

@Composable
fun CampusToggle(
    selectedCampusName: String?,
    onCampusClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.width(60.dp)) {
        CampusButton(
            label = "SGW",
            // Use equals with ignoreCase to ensure the color stays solid
            isSelected = selectedCampusName?.equals("SGW", ignoreCase = true) == true,
            isTop = true,
            onClick = { onCampusClick("SGW") }
        )
        CampusButton(
            label = "LOY",
            // Check for "LOYOLA" since that's likely the full name in your JSON
            isSelected = selectedCampusName?.equals("LOYOLA", ignoreCase = true) == true,
            isTop = false,
            onClick = { onCampusClick("LOYOLA") }
        )
    }
}

@Composable
fun CampusButton(
    label: String,
    isSelected: Boolean,
    isTop: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(60.dp).fillMaxWidth(),
        shape = if (isTop) RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        else RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) lightMaroon else ToggleWhite,
            contentColor = if (isSelected) Color.White else lightMaroon
        ),
        border = BorderStroke(1.dp, ConcordiaMaroon),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}