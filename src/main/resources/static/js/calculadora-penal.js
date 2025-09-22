// Global variables
let currentCalculation = null;

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    updateDateTime();
    setInterval(updateDateTime, 1000);
    
    // Add event listeners for form inputs
    setupFormListeners();
});

// Update date and time display
function updateDateTime() {
    const now = new Date();
    const options = {
        timeZone: 'America/Fortaleza',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    };
    
    const formatter = new Intl.DateTimeFormat('pt-BR', options);
    const dateTimeString = formatter.format(now);
    
    document.getElementById('currentDateTime').textContent = dateTimeString;
}

// Setup form event listeners
function setupFormListeners() {
    // Add change listeners to form inputs to enable/disable buttons
    const formInputs = document.querySelectorAll('#penalCalculatorForm input, #penalCalculatorForm select, #penalCalculatorForm textarea');
    formInputs.forEach(input => {
        input.addEventListener('change', validateForm);
        input.addEventListener('input', validateForm);
    });
    
    // Add animation to infraction items
    const infractionItems = document.querySelectorAll('.infraction-item');
    infractionItems.forEach(item => {
        const checkbox = item.querySelector('input[type="checkbox"]');
        checkbox.addEventListener('change', function() {
            if (this.checked) {
                item.style.background = 'rgba(0, 102, 204, 0.1)';
                item.style.borderColor = 'var(--primary-color)';
            } else {
                item.style.background = 'white';
                item.style.borderColor = '#e9ecef';
            }
        });
    });
}

// Validate form and update button states
function validateForm() {
    const agentName = document.getElementById('agentName').value.trim();
    const agentBadge = document.getElementById('agentBadge').value.trim();
    const vehicleType = document.getElementById('vehicleType').value;
    const missionType = document.getElementById('missionType').value;
    
    const isBasicFormValid = agentName && agentBadge && vehicleType && missionType;
    
    // Enable calculate button if basic form is valid
    const calculateBtn = document.querySelector('.calculate-btn');
    calculateBtn.disabled = !isBasicFormValid;
}

// Calculate penalty based on selected infractions
function calculatePenalty() {
    // Get form data
    const agentName = document.getElementById('agentName').value.trim();
    const agentBadge = document.getElementById('agentBadge').value.trim();
    const vehicleType = document.getElementById('vehicleType').value;
    const missionType = document.getElementById('missionType').value;
    const observations = document.getElementById('observations').value.trim();
    
    // Get selected infractions
    const selectedInfractions = [];
    let totalPoints = 0;
    
    const checkboxes = document.querySelectorAll('.infraction-item input[type="checkbox"]:checked');
    checkboxes.forEach(checkbox => {
        const points = parseInt(checkbox.getAttribute('data-points'));
        const label = checkbox.nextElementSibling.textContent.split('(')[0].trim();
        
        selectedInfractions.push({
            name: label,
            points: points
        });
        totalPoints += points;
    });
    
    if (selectedInfractions.length === 0) {
        alert('Selecione pelo menos uma infração para calcular a penalidade.');
        return;
    }
    
    // Determine penalty level
    let penaltyLevel = 'low';
    let penaltyDescription = 'Advertência Verbal';
    let penaltyColor = 'level-low';
    
    if (totalPoints >= 10) {
        penaltyLevel = 'high';
        penaltyDescription = 'Suspensão Disciplinar';
        penaltyColor = 'level-high';
    } else if (totalPoints >= 5) {
        penaltyLevel = 'medium';
        penaltyDescription = 'Advertência Escrita';
        penaltyColor = 'level-medium';
    }
    
    // Store calculation data
    currentCalculation = {
        agentName,
        agentBadge,
        vehicleType,
        missionType,
        observations,
        infractions: selectedInfractions,
        totalPoints,
        penaltyLevel,
        penaltyDescription,
        calculationDate: new Date().toLocaleString('pt-BR', {
            timeZone: 'America/Fortaleza',
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        })
    };
    
    // Display results
    displayResults(currentCalculation);
    
    // Enable report generation buttons
    document.getElementById('generateReportBtn').disabled = false;
    document.getElementById('exportExcelBtn').disabled = false;
    
    // Show notification
    showNotification('Cálculo realizado com sucesso!', 'success');
}

// Display calculation results
function displayResults(calculation) {
    const resultsContainer = document.getElementById('calculationResults');
    
    const resultsHTML = `
        <div class="calculation-result fade-in">
            <div class="penalty-score">
                <span class="score-number">${calculation.totalPoints}</span>
                <span class="score-label">Pontos de Penalidade</span>
            </div>
            
            <div class="penalty-level ${calculation.penaltyLevel === 'low' ? 'level-low' : calculation.penaltyLevel === 'medium' ? 'level-medium' : 'level-high'}">
                <strong>${calculation.penaltyDescription}</strong>
            </div>
            
            <div class="infractions-list">
                <h6><i class="fas fa-list"></i> Infrações Registradas:</h6>
                <ul>
                    ${calculation.infractions.map(infraction => 
                        `<li>${infraction.name} - ${infraction.points} pts</li>`
                    ).join('')}
                </ul>
            </div>
        </div>
    `;
    
    resultsContainer.innerHTML = resultsHTML;
    
    // Generate report preview
    generateReportPreview(calculation);
}

// Generate report preview
function generateReportPreview(calculation) {
    const reportContainer = document.getElementById('reportPreview');
    
    const reportHTML = `
        <div class="report-content fade-in">
            <div class="report-header">
                <h4>RELATÓRIO DE INFRAÇÃO DISCIPLINAR</h4>
                <div class="report-date">G.R.A - Grupo de Radiopatrulhamento Aéreo</div>
                <div class="report-date">Data: ${calculation.calculationDate}</div>
            </div>
            
            <div class="report-section">
                <h6><i class="fas fa-user"></i> Dados do Agente</h6>
                <div class="report-field">
                    <span><strong>Nome:</strong></span>
                    <span>${calculation.agentName}</span>
                </div>
                <div class="report-field">
                    <span><strong>Placa:</strong></span>
                    <span>${calculation.agentBadge}</span>
                </div>
            </div>
            
            <div class="report-section">
                <h6><i class="fas fa-helicopter"></i> Dados da Operação</h6>
                <div class="report-field">
                    <span><strong>Veículo:</strong></span>
                    <span>${calculation.vehicleType}</span>
                </div>
                <div class="report-field">
                    <span><strong>Tipo de Missão:</strong></span>
                    <span>${calculation.missionType}</span>
                </div>
            </div>
            
            <div class="report-section">
                <h6><i class="fas fa-exclamation-triangle"></i> Infrações Cometidas</h6>
                ${calculation.infractions.map(infraction => `
                    <div class="report-field">
                        <span><strong>${infraction.name}:</strong></span>
                        <span>${infraction.points} pontos</span>
                    </div>
                `).join('')}
                <div class="report-field" style="border-top: 1px solid #dee2e6; padding-top: 10px; margin-top: 10px;">
                    <span><strong>Total de Pontos:</strong></span>
                    <span style="color: var(--danger-color); font-weight: 700;">${calculation.totalPoints} pontos</span>
                </div>
            </div>
            
            <div class="report-section">
                <h6><i class="fas fa-gavel"></i> Penalidade Aplicada</h6>
                <div class="report-field">
                    <span><strong>Sanção:</strong></span>
                    <span style="color: var(--primary-color); font-weight: 600;">${calculation.penaltyDescription}</span>
                </div>
            </div>
            
            ${calculation.observations ? `
                <div class="report-section">
                    <h6><i class="fas fa-sticky-note"></i> Observações</h6>
                    <p style="margin: 0; padding: 10px; background: #f8f9fa; border-radius: 4px;">${calculation.observations}</p>
                </div>
            ` : ''}
            
            <div class="report-section" style="margin-top: 40px; text-align: center; border-top: 2px solid var(--primary-color); padding-top: 20px;">
                <p style="margin: 0; color: #6c757d; font-size: 0.9rem;">
                    Este relatório foi gerado automaticamente pelo Sistema de Calculadora Penal G.R.A<br>
                    Data/Hora de geração: ${new Date().toLocaleString('pt-BR', { timeZone: 'America/Fortaleza' })}
                </p>
            </div>
        </div>
    `;
    
    reportContainer.innerHTML = reportHTML;
}

// Generate and download PDF report
function generateReport() {
    if (!currentCalculation) {
        alert('Realize um cálculo antes de gerar o relatório.');
        return;
    }
    
    // Create a new window for printing
    const printWindow = window.open('', '_blank');
    const reportContent = document.getElementById('reportPreview').innerHTML;
    
    printWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>Relatório de Infração - ${currentCalculation.agentName}</title>
            <style>
                body { 
                    font-family: Arial, sans-serif; 
                    padding: 20px; 
                    background: white;
                }
                .report-content { 
                    max-width: 800px; 
                    margin: 0 auto; 
                    background: white;
                    border: none;
                    border-radius: 0;
                    padding: 0;
                }
                .report-header { 
                    text-align: center; 
                    margin-bottom: 30px; 
                    padding-bottom: 20px; 
                    border-bottom: 2px solid #0066cc; 
                }
                .report-header h4 { 
                    color: #0066cc; 
                    font-weight: 700; 
                    margin-bottom: 5px; 
                }
                .report-section { 
                    margin-bottom: 25px; 
                }
                .report-section h6 { 
                    color: #004080; 
                    font-weight: 600; 
                    margin-bottom: 10px; 
                    padding-bottom: 5px; 
                    border-bottom: 1px solid #dee2e6; 
                }
                .report-field { 
                    display: flex; 
                    justify-content: space-between; 
                    margin-bottom: 8px; 
                    font-size: 0.95rem; 
                }
                @media print {
                    body { margin: 0; padding: 20px; }
                    .report-content { box-shadow: none; }
                }
            </style>
        </head>
        <body>
            ${reportContent}
        </body>
        </html>
    `);
    
    printWindow.document.close();
    
    // Focus the window and print
    printWindow.focus();
    setTimeout(() => {
        printWindow.print();
    }, 250);
    
    showNotification('Relatório gerado com sucesso! Use Ctrl+P para imprimir ou salvar como PDF.', 'success');
}

// Export to Excel (CSV format)
function exportToExcel() {
    if (!currentCalculation) {
        alert('Realize um cálculo antes de exportar.');
        return;
    }
    
    const calc = currentCalculation;
    
    // Create CSV content
    let csvContent = "data:text/csv;charset=utf-8,";
    csvContent += "RELATÓRIO DE INFRAÇÃO DISCIPLINAR - G.R.A\n\n";
    csvContent += "Campo,Valor\n";
    csvContent += `Data do Relatório,${calc.calculationDate}\n`;
    csvContent += `Nome do Agente,${calc.agentName}\n`;
    csvContent += `Número da Placa,${calc.agentBadge}\n`;
    csvContent += `Tipo de Veículo,${calc.vehicleType}\n`;
    csvContent += `Tipo de Missão,${calc.missionType}\n\n`;
    csvContent += "INFRAÇÕES COMETIDAS\n";
    csvContent += "Infração,Pontos\n";
    
    calc.infractions.forEach(infraction => {
        csvContent += `"${infraction.name}",${infraction.points}\n`;
    });
    
    csvContent += `\nTotal de Pontos,${calc.totalPoints}\n`;
    csvContent += `Penalidade Aplicada,"${calc.penaltyDescription}"\n`;
    
    if (calc.observations) {
        csvContent += `\nObservações,"${calc.observations}"\n`;
    }
    
    // Create and download file
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `relatorio_infracao_${calc.agentBadge}_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    showNotification('Arquivo Excel exportado com sucesso!', 'success');
}

// Clear form and reset everything
function clearForm() {
    if (confirm('Tem certeza que deseja limpar todos os dados do formulário?')) {
        // Reset form
        document.getElementById('penalCalculatorForm').reset();
        
        // Clear results
        document.getElementById('calculationResults').innerHTML = `
            <div class="no-calculation">
                <i class="fas fa-info-circle"></i>
                <p>Preencha os dados e clique em "Calcular Penalidade" para ver os resultados.</p>
            </div>
        `;
        
        // Clear report preview
        document.getElementById('reportPreview').innerHTML = `
            <div class="no-report">
                <i class="fas fa-file"></i>
                <p>O relatório será exibido aqui após o cálculo.</p>
            </div>
        `;
        
        // Disable report buttons
        document.getElementById('generateReportBtn').disabled = true;
        document.getElementById('exportExcelBtn').disabled = true;
        
        // Reset current calculation
        currentCalculation = null;
        
        // Reset infraction item styles
        const infractionItems = document.querySelectorAll('.infraction-item');
        infractionItems.forEach(item => {
            item.style.background = 'white';
            item.style.borderColor = '#e9ecef';
        });
        
        showNotification('Formulário limpo com sucesso!', 'info');
    }
}

// Show notification
function showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `alert alert-${type === 'success' ? 'success' : type === 'error' ? 'danger' : 'info'} alert-dismissible fade show position-fixed`;
    notification.style.cssText = `
        top: 20px;
        right: 20px;
        z-index: 9999;
        min-width: 300px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.2);
    `;
    
    notification.innerHTML = `
        <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.appendChild(notification);
    
    // Auto remove after 5 seconds
    setTimeout(() => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    }, 5000);
}

// Prevent form submission on Enter key
document.addEventListener('keydown', function(e) {
    if (e.key === 'Enter' && e.target.tagName !== 'TEXTAREA' && e.target.tagName !== 'BUTTON') {
        e.preventDefault();
    }
});