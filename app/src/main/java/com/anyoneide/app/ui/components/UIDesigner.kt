package com.anyoneide.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UIDesigner(
    modifier: Modifier = Modifier,
    onCodeGenerated: (String) -> Unit = {}
) {
    var selectedComponent by remember { mutableStateOf<UIComponent?>(null) }
    var components by remember { mutableStateOf(listOf<UIComponent>()) }
    var showProperties by remember { mutableStateOf(false) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    var layoutName by remember { mutableStateOf("activity_main") }
    var isDragging by remember { mutableStateOf(false) }

    Row(modifier = modifier.fillMaxSize()) {
        // Component Palette
        ComponentPalette(
            modifier = Modifier.width(250.dp).fillMaxHeight(),
            onComponentSelected = { component ->
                components = components + component.copy(
                    id = "component_${System.currentTimeMillis()}",
                    x = 100f,
                    y = 100f
                )
            }
        )

        // Design Canvas
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            // Toolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UI Designer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row {
                        // Layout name field
                        OutlinedTextField(
                            value = layoutName,
                            onValueChange = { layoutName = it },
                            label = { Text("Layout Name") },
                            modifier = Modifier.width(200.dp),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Generate XML button
                        Button(
                            onClick = { showGenerateDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate XML")
                        }
                    }
                }
            }
            
            // Canvas area with grid background
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.White)
            ) {
                // Grid background
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) { 
                    drawGrid(this)
                }
                
                // Components
                components.forEach { component ->
                    val isSelected = selectedComponent?.id == component.id
                    
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(component.x.roundToInt(), component.y.roundToInt()) }
                            .size(
                                width = component.width.dp,
                                height = component.height.dp
                            )
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { 
                                selectedComponent = component
                                showProperties = true
                            }
                            .pointerInput(component.id) {
                                detectDragGestures(
                                    onDragStart = { 
                                        selectedComponent = component
                                        showProperties = true
                                        isDragging = true
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        
                                        // Update component position
                                        val updatedComponent = component.copy(
                                            x = (component.x + dragAmount.x).coerceIn(0f, 1000f),
                                            y = (component.y + dragAmount.y).coerceIn(0f, 1000f)
                                        )
                                        
                                        // Update the component in the list
                                        components = components.map { 
                                            if (it.id == component.id) updatedComponent else it 
                                        }
                                        
                                        // Update selected component if it's the one being dragged
                                        if (selectedComponent?.id == component.id) {
                                            selectedComponent = updatedComponent
                                        }
                                    }
                                )
                            }
                    ) {
                        ComponentPreview(component = component)
                    }
                }
                
                // Instructions when no components
                if (components.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DesignServices,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Drag components from the palette",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Properties Panel
        if (showProperties && selectedComponent != null) {
            PropertiesPanel(
                modifier = Modifier.width(300.dp).fillMaxHeight(),
                component = selectedComponent!!,
                onPropertyChanged = { property, value ->
                    components = components.map { component ->
                        if (component.id == selectedComponent!!.id) {
                            component.copy(properties = component.properties + (property to value))
                        } else {
                            component
                        }
                    }
                    selectedComponent = selectedComponent!!.copy(
                        properties = selectedComponent!!.properties + (property to value)
                    )
                },
                onClose = { showProperties = false },
                onGenerateCode = { showGenerateDialog = true },
                onDeleteComponent = {
                    components = components.filter { it.id != selectedComponent!!.id }
                    selectedComponent = null
                    showProperties = false
                },
                onUpdateSize = { width, height ->
                    components = components.map { component ->
                        if (component.id == selectedComponent!!.id) {
                            component.copy(width = width, height = height)
                        } else {
                            component
                        }
                    }
                    selectedComponent = selectedComponent!!.copy(width = width, height = height)
                }
            )
        }
    }
    
    // Generate XML Dialog
    if (showGenerateDialog) {
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text("Generate XML Layout") },
            text = {
                Column {
                    Text("Generate XML layout with the following name:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = layoutName,
                        onValueChange = { layoutName = it },
                        label = { Text("Layout Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The file will be saved as: ${layoutName}.xml",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val xmlCode = generateXmlCode(components, layoutName)
                        onCodeGenerated(xmlCode)
                        showGenerateDialog = false
                    }
                ) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGenerateDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ComponentPalette(
    modifier: Modifier = Modifier,
    onComponentSelected: (UIComponent) -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Text(
                text = "Components",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableComponents) { component ->
                    ComponentItem(
                        component = component,
                        onClick = { onComponentSelected(component) }
                    )
                }
            }
        }
    }
}

@Composable
fun ComponentItem(
    component: UIComponent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = component.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = component.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = component.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ComponentPreview(component: UIComponent) {
    when (component.type) {
        ComponentType.TEXT_VIEW -> {
            Text(
                text = component.properties["text"] ?: "TextView",
                modifier = Modifier.padding(8.dp),
                fontSize = 14.sp
            )
        }
        ComponentType.BUTTON -> {
            Button(
                onClick = { },
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = component.properties["text"] ?: "Button",
                    fontSize = 12.sp
                )
            }
        }
        ComponentType.EDIT_TEXT -> {
            OutlinedTextField(
                value = component.properties["hint"] ?: "EditText",
                onValueChange = { },
                modifier = Modifier.padding(4.dp),
                enabled = false
            )
        }
        ComponentType.IMAGE_VIEW -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "ImageView",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        ComponentType.LINEAR_LAYOUT -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color.Blue, RoundedCornerShape(4.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LinearLayout",
                    fontSize = 10.sp,
                    color = Color.Blue
                )
            }
        }
        ComponentType.CONSTRAINT_LAYOUT -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color.Green, RoundedCornerShape(4.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ConstraintLayout",
                    fontSize = 10.sp,
                    color = Color.Green
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesPanel(
    modifier: Modifier = Modifier,
    component: UIComponent,
    onPropertyChanged: (String, String) -> Unit,
    onClose: () -> Unit,
    onGenerateCode: () -> Unit,
    onDeleteComponent: () -> Unit,
    onUpdateSize: (Float, Float) -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Properties",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Component info
            Text(
                text = component.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Position and size
            Text(
                text = "Position and Size",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // X position
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "X Position",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${component.x.roundToInt()}dp",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Y position
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Y Position",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${component.y.roundToInt()}dp",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Width and height sliders
            Text(
                text = "Width: ${component.width.roundToInt()}dp",
                style = MaterialTheme.typography.bodySmall
            )
            
            Slider(
                value = component.width,
                onValueChange = { newWidth ->
                    onUpdateSize(newWidth, component.height)
                },
                valueRange = 48f..400f
            )
            
            Text(
                text = "Height: ${component.height.roundToInt()}dp",
                style = MaterialTheme.typography.bodySmall
            )
            
            Slider(
                value = component.height,
                onValueChange = { newHeight ->
                    onUpdateSize(component.width, newHeight)
                },
                valueRange = 48f..400f
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Properties
            Text(
                text = "Properties",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Properties list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(getEditableProperties(component.type)) { property ->
                    PropertyEditor(
                        property = property,
                        value = component.properties[property.key] ?: property.defaultValue,
                        onValueChanged = { newValue ->
                            onPropertyChanged(property.key, newValue)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Delete button
                Button(
                    onClick = onDeleteComponent,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
                
                // Generate XML button
                Button(
                    onClick = onGenerateCode
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate XML")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyEditor(
    property: ComponentProperty,
    value: String,
    onValueChanged: (String) -> Unit
) {
    Column {
        Text(
            text = property.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        when (property.type) {
            PropertyType.TEXT -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            PropertyType.NUMBER -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == '.' }) {
                            onValueChanged(newValue)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            PropertyType.BOOLEAN -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = value.toBoolean(),
                        onCheckedChange = { onValueChanged(it.toString()) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (value.toBoolean()) "True" else "False",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            PropertyType.DROPDOWN -> {
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        property.options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onValueChanged(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Draw grid for the canvas
private fun drawGrid(drawScope: DrawScope) {
    val gridSize = 20f
    val width = drawScope.size.width
    val height = drawScope.size.height
    
    // Draw vertical lines
    var x = 0f
    while (x <= width) {
        drawScope.drawLine(
            color = Color.LightGray.copy(alpha = 0.3f),
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
        x += gridSize
    }
    
    // Draw horizontal lines
    var y = 0f
    while (y <= height) {
        drawScope.drawLine(
            color = Color.LightGray.copy(alpha = 0.3f),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += gridSize
    }
}

private fun generateXmlCode(components: List<UIComponent>, layoutName: String): String {
    val sb = StringBuilder()
    sb.appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
    sb.appendLine("<!-- Generated layout for $layoutName -->")
    sb.appendLine("<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"")
    sb.appendLine("    xmlns:app=\"http://schemas.android.com/apk/res-auto\"")
    sb.appendLine("    xmlns:tools=\"http://schemas.android.com/tools\"")
    sb.appendLine("    android:layout_width=\"match_parent\"")
    sb.appendLine("    android:layout_height=\"match_parent\">")
    sb.appendLine()
    
    components.forEach { component ->
        when (component.type) {
            ComponentType.TEXT_VIEW -> {
                sb.appendLine("    <TextView")
                sb.appendLine("        android:id=\"@+id/${getComponentId(component)}\"")
                sb.appendLine("        android:layout_width=\"${component.width.roundToInt()}dp\"")
                sb.appendLine("        android:layout_height=\"${component.height.roundToInt()}dp\"")
                sb.appendLine("        android:text=\"${component.properties["text"] ?: "TextView"}\"")
                sb.appendLine("        android:layout_marginTop=\"${component.y.roundToInt()}dp\"")
                sb.appendLine("        android:layout_marginStart=\"${component.x.roundToInt()}dp\" />")
            }
            ComponentType.BUTTON -> {
                sb.appendLine("    <Button")
                sb.appendLine("        android:id=\"@+id/${getComponentId(component)}\"")
                sb.appendLine("        android:layout_width=\"${component.width.roundToInt()}dp\"")
                sb.appendLine("        android:layout_height=\"${component.height.roundToInt()}dp\"")
                sb.appendLine("        android:text=\"${component.properties["text"] ?: "Button"}\"")
                sb.appendLine("        android:layout_marginTop=\"${component.y.roundToInt()}dp\"")
                sb.appendLine("        android:layout_marginStart=\"${component.x.roundToInt()}dp\" />")
            }
            ComponentType.EDIT_TEXT -> {
                sb.appendLine("    <EditText")
                sb.appendLine("        android:id=\"@+id/${getComponentId(component)}\"")
                sb.appendLine("        android:layout_width=\"${component.width.roundToInt()}dp\"")
                sb.appendLine("        android:layout_height=\"${component.height.roundToInt()}dp\"")
                sb.appendLine("        android:hint=\"${component.properties["hint"] ?: "Enter text"}\"")
                sb.appendLine("        android:inputType=\"${component.properties["inputType"] ?: "text"}\"")
                sb.appendLine("        android:layout_marginTop=\"${component.y.roundToInt()}dp\"")
                sb.appendLine("        android:layout_marginStart=\"${component.x.roundToInt()}dp\" />")
            }
            ComponentType.IMAGE_VIEW -> {
                sb.appendLine("    <ImageView")
                sb.appendLine("        android:id=\"@+id/${getComponentId(component)}\"")
                sb.appendLine("        android:layout_width=\"${component.width.roundToInt()}dp\"")
                sb.appendLine("        android:layout_height=\"${component.height.roundToInt()}dp\"")
                sb.appendLine("        android:src=\"@drawable/ic_placeholder\"")
                sb.appendLine("        android:scaleType=\"${component.properties["scaleType"] ?: "fitCenter"}\"")
                sb.appendLine("        android:layout_marginTop=\"${component.y.roundToInt()}dp\"")
                sb.appendLine("        android:layout_marginStart=\"${component.x.roundToInt()}dp\" />")
            }
            ComponentType.LINEAR_LAYOUT -> {
                sb.appendLine("    <LinearLayout")
                sb.appendLine("        android:id=\"@+id/${getComponentId(component)}\"")
                sb.appendLine("        android:layout_width=\"${component.width.roundToInt()}dp\"")
                sb.appendLine("        android:layout_height=\"${component.height.roundToInt()}dp\"")
                sb.appendLine("        android:orientation=\"${component.properties["orientation"] ?: "vertical"}\"")
                sb.appendLine("        android:layout_marginTop=\"${component.y.roundToInt()}dp\"")
                sb.appendLine("        android:layout_marginStart=\"${component.x.roundToInt()}dp\">")
                sb.appendLine("    </LinearLayout>")
            }
            ComponentType.CONSTRAINT_LAYOUT -> {
                sb.appendLine("    <androidx.constraintlayout.widget.ConstraintLayout")
                sb.appendLine("        android:id=\"@+id/${getComponentId(component)}\"")
                sb.appendLine("        android:layout_width=\"${component.width.roundToInt()}dp\"")
                sb.appendLine("        android:layout_height=\"${component.height.roundToInt()}dp\"")
                sb.appendLine("        android:layout_marginTop=\"${component.y.roundToInt()}dp\"")
                sb.appendLine("        android:layout_marginStart=\"${component.x.roundToInt()}dp\">")
                sb.appendLine("    </androidx.constraintlayout.widget.ConstraintLayout>")
            }
        }
        sb.appendLine()
    }
    
    sb.appendLine("</RelativeLayout>")
    return sb.toString()
}

private fun getComponentId(component: UIComponent): String {
    val baseName = when (component.type) {
        ComponentType.TEXT_VIEW -> "textView"
        ComponentType.BUTTON -> "button"
        ComponentType.EDIT_TEXT -> "editText"
        ComponentType.IMAGE_VIEW -> "imageView"
        ComponentType.LINEAR_LAYOUT -> "linearLayout"
        ComponentType.CONSTRAINT_LAYOUT -> "constraintLayout"
    }
    
    // Extract numeric part of ID for a unique identifier
    val idNumber = component.id.replace(Regex("[^0-9]"), "")
    return "${baseName}_$idNumber"
}

private fun getEditableProperties(componentType: ComponentType): List<ComponentProperty> {
    return when (componentType) {
        ComponentType.TEXT_VIEW -> listOf(
            ComponentProperty("text", "Text", PropertyType.TEXT, "TextView"),
            ComponentProperty("textSize", "Text Size", PropertyType.NUMBER, "14"),
            ComponentProperty("textColor", "Text Color", PropertyType.TEXT, "#000000"),
            ComponentProperty("textStyle", "Text Style", PropertyType.DROPDOWN, "normal", 
                listOf("normal", "bold", "italic", "bold|italic"))
        )
        ComponentType.BUTTON -> listOf(
            ComponentProperty("text", "Text", PropertyType.TEXT, "Button"),
            ComponentProperty("enabled", "Enabled", PropertyType.BOOLEAN, "true"),
            ComponentProperty("textColor", "Text Color", PropertyType.TEXT, "#FFFFFF"),
            ComponentProperty("style", "Style", PropertyType.DROPDOWN, "default", 
                listOf("default", "outlined", "text"))
        )
        ComponentType.EDIT_TEXT -> listOf(
            ComponentProperty("hint", "Hint", PropertyType.TEXT, "Enter text"),
            ComponentProperty("inputType", "Input Type", PropertyType.DROPDOWN, "text", 
                listOf("text", "number", "email", "password", "phone")),
            ComponentProperty("singleLine", "Single Line", PropertyType.BOOLEAN, "true"),
            ComponentProperty("maxLength", "Max Length", PropertyType.NUMBER, "100")
        )
        ComponentType.IMAGE_VIEW -> listOf(
            ComponentProperty("scaleType", "Scale Type", PropertyType.DROPDOWN, "fitCenter",
                listOf("fitCenter", "centerCrop", "centerInside", "fitXY", "fitStart", "fitEnd")),
            ComponentProperty("contentDescription", "Content Description", PropertyType.TEXT, "Image"),
            ComponentProperty("tint", "Tint Color", PropertyType.TEXT, ""),
            ComponentProperty("adjustViewBounds", "Adjust View Bounds", PropertyType.BOOLEAN, "false")
        )
        ComponentType.LINEAR_LAYOUT -> listOf(
            ComponentProperty("orientation", "Orientation", PropertyType.DROPDOWN, "vertical",
                listOf("vertical", "horizontal")),
            ComponentProperty("gravity", "Gravity", PropertyType.DROPDOWN, "start|top",
                listOf("start|top", "center", "end|bottom", "center_horizontal", "center_vertical")),
            ComponentProperty("padding", "Padding", PropertyType.NUMBER, "8"),
            ComponentProperty("background", "Background Color", PropertyType.TEXT, "#FFFFFF")
        )
        ComponentType.CONSTRAINT_LAYOUT -> listOf(
            ComponentProperty("padding", "Padding", PropertyType.NUMBER, "8"),
            ComponentProperty("background", "Background Color", PropertyType.TEXT, "#FFFFFF")
        )
    }
}

// Data classes
data class UIComponent(
    val id: String = "",
    val name: String,
    val description: String,
    val type: ComponentType,
    val icon: ImageVector,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 120f,
    val height: Float = 48f,
    val properties: Map<String, String> = emptyMap()
)

enum class ComponentType {
    TEXT_VIEW,
    BUTTON,
    EDIT_TEXT,
    IMAGE_VIEW,
    LINEAR_LAYOUT,
    CONSTRAINT_LAYOUT
}

data class ComponentProperty(
    val key: String,
    val name: String,
    val type: PropertyType,
    val defaultValue: String,
    val options: List<String> = emptyList()
)

enum class PropertyType {
    TEXT,
    NUMBER,
    BOOLEAN,
    DROPDOWN
}

private val availableComponents = listOf(
    UIComponent(
        name = "TextView",
        description = "Display text",
        type = ComponentType.TEXT_VIEW,
        icon = Icons.Default.TextFields
    ),
    UIComponent(
        name = "Button",
        description = "Clickable button",
        type = ComponentType.BUTTON,
        icon = Icons.Default.SmartButton
    ),
    UIComponent(
        name = "EditText",
        description = "Text input field",
        type = ComponentType.EDIT_TEXT,
        icon = Icons.Default.Edit
    ),
    UIComponent(
        name = "ImageView",
        description = "Display images",
        type = ComponentType.IMAGE_VIEW,
        icon = Icons.Default.Image
    ),
    UIComponent(
        name = "LinearLayout",
        description = "Linear container",
        type = ComponentType.LINEAR_LAYOUT,
        icon = Icons.Default.ViewColumn
    ),
    UIComponent(
        name = "ConstraintLayout",
        description = "Constraint container",
        type = ComponentType.CONSTRAINT_LAYOUT,
        icon = Icons.Default.GridView
    )
)