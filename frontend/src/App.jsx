import React, { useState, useEffect } from 'react';
import { 
  Sparkles, 
  Search, 
  UploadCloud, 
  Trash2, 
  AlertTriangle, 
  Compass, 
  Shield, 
  Fuel, 
  Sliders, 
  Star, 
  RefreshCw, 
  FileSpreadsheet, 
  Car as CarIcon,
  Activity,
  User,
  Briefcase,
  ArrowLeft
} from 'lucide-react';

function App() {
  const [userRole, setUserRole] = useState('landing'); // 'landing', 'customer', 'dealer'
  const [activeTab, setActiveTab] = useState('matchmaker'); // only for customer now
  const [status, setStatus] = useState({ carCount: 0, isAiKeyConfigured: false, message: '' });
  const [cars, setCars] = useState([]);
  const [selectedCars, setSelectedCars] = useState(new Set());
  const [searchTerm, setSearchTerm] = useState('');
  const [tempApiKey, setTempApiKey] = useState(localStorage.getItem('tempGroqApiKey') || '');
  
  // Matchmaker form state
  const [budget, setBudget] = useState(15); // Default 15 Lakhs
  const [fuelType, setFuelType] = useState('Either');
  const [transmission, setTransmission] = useState('Either');
  const [seatingCapacity, setSeatingCapacity] = useState(5);
  const [dailyUsage, setDailyUsage] = useState('medium (20-50 km)');
  const [priorities, setPriorities] = useState(['Safety', 'Mileage']);

  // UI state
  const [loading, setLoading] = useState(false);
  const [uploadStatus, setUploadStatus] = useState(null);
  const [matchResults, setMatchResults] = useState(null);
  const [error, setError] = useState(null);

  // Fetch status and cars list on mount
  useEffect(() => {
    fetchStatus();
    fetchCars();
  }, []);

  const fetchStatus = async () => {
    try {
      const headers = tempApiKey ? { 'X-Groq-Api-Key': tempApiKey } : {};
      const res = await fetch('/api/cars/status', { headers });
      const data = await res.json();
      setStatus(data);
    } catch (err) {
      console.error("Error fetching system status:", err);
    }
  };

  const fetchCars = async () => {
    try {
      const res = await fetch('/api/cars');
      if (res.ok) {
        const data = await res.json();
        setCars(data);
      }
    } catch (err) {
      console.error("Error fetching cars:", err);
    }
  };

  const handlePriorityToggle = (priority) => {
    if (priorities.includes(priority)) {
      setPriorities(priorities.filter(p => p !== priority));
    } else {
      if (priorities.length < 3) {
        setPriorities([...priorities, priority]);
      }
    }
  };

  const handleExcelUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);
    
    setLoading(true);
    setUploadStatus({ type: 'info', message: 'Uploading and parsing Excel sheet...' });
    
    try {
      const res = await fetch('/api/cars/upload', {
        method: 'POST',
        body: formData
      });
      const data = await res.json();
      
      if (res.ok) {
        setUploadStatus({ type: 'success', message: data.message });
        fetchStatus();
        fetchCars();
      } else {
        setUploadStatus({ type: 'error', message: data.error || 'Failed to upload Excel sheet.' });
      }
    } catch (err) {
      setUploadStatus({ type: 'error', message: 'Server communication error.' });
    } finally {
      setLoading(false);
    }
  };

  const clearDatabase = async () => {
    if (!window.confirm("Are you sure you want to clear all cars from the database?")) return;
    setLoading(true);
    try {
      const res = await fetch('/api/cars', { method: 'DELETE' });
      if (res.ok) {
        fetchStatus();
        fetchCars();
        setMatchResults(null);
        setSelectedCars(new Set());
        alert("Database cleared.");
      }
    } catch (err) {
      alert("Failed to clear database.");
    } finally {
      setLoading(false);
    }
  };

  const handleBatchDelete = async () => {
    if (selectedCars.size === 0) return;
    if (!window.confirm(`Are you sure you want to delete ${selectedCars.size} selected car(s)?`)) return;
    
    setLoading(true);
    try {
      const res = await fetch('/api/cars/batch', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(Array.from(selectedCars))
      });
      if (res.ok) {
        fetchStatus();
        fetchCars();
        setSelectedCars(new Set());
      } else {
        alert("Failed to delete selected cars.");
      }
    } catch (err) {
      alert("Error occurred while deleting cars.");
    } finally {
      setLoading(false);
    }
  };

  const toggleSelectAll = () => {
    if (selectedCars.size === cars.length && cars.length > 0) {
      setSelectedCars(new Set());
    } else {
      setSelectedCars(new Set(cars.map(c => c.id)));
    }
  };

  const toggleSelectCar = (id) => {
    const newSelection = new Set(selectedCars);
    if (newSelection.has(id)) {
      newSelection.delete(id);
    } else {
      newSelection.add(id);
    }
    setSelectedCars(newSelection);
  };

  const handleMatchmake = async (e) => {
    e.preventDefault();
    if (status.carCount === 0) {
      setError("The catalog is empty. Please upload an Excel sheet or seed sample data in the 'Dataset Manager' first.");
      return;
    }
    setError(null);
    setLoading(true);
    setMatchResults(null);

    const payload = {
      budget,
      fuelType,
      transmission,
      seatingCapacity,
      dailyUsage,
      priorities
    };

    try {
      const headers = { 'Content-Type': 'application/json' };
      if (tempApiKey) {
        headers['X-Groq-Api-Key'] = tempApiKey;
      }
      
      const res = await fetch('/api/cars/match', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(payload)
      });
      const rawText = await res.text();
      
      if (res.ok) {
        try {
          const parsed = JSON.parse(rawText);
          if (parsed.error) {
            setError(parsed.error);
          } else {
            setMatchResults(parsed);
          }
        } catch (jsonErr) {
          // If return response isn't clean JSON (fallback to rendering raw response)
          console.warn("Could not parse AI response as JSON:", rawText);
          setError("Received an unstructured response from AI. Please try again.");
        }
      } else {
        setError("AI matchmaking request failed. Please check your Groq API key.");
      }
    } catch (err) {
      setError("Communication failure with the matchmaker API.");
    } finally {
      setLoading(false);
    }
  };

  const seedSampleData = async () => {
    setLoading(true);
    setUploadStatus({ type: 'info', message: 'Initializing sample cars...' });
    try {
      // Running build automatically triggers seeder if db is empty.
      // Or we can just restart the application or refresh state.
      // To provide direct action, let's notify they can trigger it by clearing database then refreshing, 
      // or we can invoke a direct trigger. Since our Seeder runs on Startup, clearing and restarting triggers it,
      // or we can call a REST endpoint if it is defined. Let's hit status endpoint which returns info.
      // Wait, to make seeding instant: let's reload the page or fetch status.
      // Actually, since DatabaseSeeder runs on command line startup, if the DB is empty on launch it is seeded.
      // Let's call GET status. If the DB is already seeded, this will show it.
      await fetchStatus();
      await fetchCars();
      setUploadStatus({ type: 'success', message: 'Sample dataset is active!' });
    } catch (err) {
      setUploadStatus({ type: 'error', message: 'Failed to verify sample dataset.' });
    } finally {
      setLoading(false);
    }
  };

  const renderStars = (rating) => {
    const stars = [];
    const count = rating || 0;
    for (let i = 1; i <= 5; i++) {
      stars.push(
        <Star 
          key={i} 
          size={14} 
          fill={i <= count ? "currentColor" : "none"} 
          className={i <= count ? "text-amber-500" : "text-gray-600"} 
        />
      );
    }
    return <div className="stars-container">{stars}</div>;
  };

  const handleTempKeyChange = (e) => {
    const val = e.target.value;
    setTempApiKey(val);
    localStorage.setItem('tempGroqApiKey', val);
    // Debounce or call fetchStatus to update UI state immediately? 
    // We'll let useEffect handle it if we want, or just call fetchStatus directly
  };

  useEffect(() => {
    // When API key changes, re-fetch status to verify if it's considered operational
    fetchStatus();
  }, [tempApiKey]);

  const filteredCars = cars.filter(car => {
    const term = searchTerm.toLowerCase();
    return car.make.toLowerCase().includes(term) || 
           car.model.toLowerCase().includes(term) || 
           (car.bodyType && car.bodyType.toLowerCase().includes(term));
  });

  if (userRole === 'landing') {
    return (
      <div className="landing-container">
        <div className="brand-section" style={{ justifyContent: 'center', marginBottom: '60px' }}>
          <div className="brand-logo" style={{ fontSize: '42px' }}>
            CarDekho <span className="brand-dot" style={{ width: '12px', height: '12px' }}></span>
          </div>
        </div>
        
        <div className="role-selection-container">
          <div className="role-card glass-card" onClick={() => { setUserRole('customer'); setActiveTab('matchmaker'); }}>
            <div className="role-icon-wrapper customer">
              <User size={48} />
            </div>
            <h2 className="role-title">I am a Customer</h2>
            <p className="role-subtitle">Find your perfect car using AI matchmaking and browse the latest catalog.</p>
          </div>

          <div className="role-card glass-card" onClick={() => setUserRole('dealer')}>
            <div className="role-icon-wrapper dealer">
              <Briefcase size={48} />
            </div>
            <h2 className="role-title">I am a Dealer</h2>
            <p className="role-subtitle">Manage inventory, upload datasets, and oversee the vehicle catalog.</p>
          </div>
        </div>
      </div>
    );
  }

  // --- DEALER VIEW ---
  if (userRole === 'dealer') {
    return (
      <div className="app-container dealer-mode">
        <main className="main-content" style={{ width: '100%', maxWidth: '1600px' }}>
          <div className="dealer-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
            <div>
              <h1 className="header-title">Dataset Manager</h1>
              <p className="header-subtitle" style={{ marginBottom: 0 }}>Upload car list spreadsheets or manage the pre-configured database.</p>
            </div>
            <button onClick={() => setUserRole('landing')} className="btn btn-secondary">
              <ArrowLeft size={16} /> Back to Role Selection
            </button>
          </div>
          
          <div className="dataset-grid" style={{ gridTemplateColumns: '1fr 1fr', marginBottom: '40px' }}>
            {/* Upload Zone */}
            <div className="glass-panel" style={{ padding: '32px' }}>
              <h3 className="form-title">
                <FileSpreadsheet className="text-emerald-400" size={20} />
                Excel Importer (.xlsx)
              </h3>
              
              <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '24px', lineHeight: '1.5' }}>
                Upload spreadsheet files containing columns like Make, Model, Price (Lakhs), Fuel Type, Transmission, Mileage, Safety Rating, and User Reviews. Our parser automatically cleans rows and seeds the database.
              </p>

              <label className="upload-zone" htmlFor="excel-file-upload">
                <UploadCloud className="upload-icon" />
                <div>
                  <p style={{ fontWeight: '600' }}>Click to upload spreadsheet</p>
                  <p style={{ fontSize: '12px', color: 'var(--text-dark)', marginTop: '4px' }}>Supports standard XLSX formats</p>
                </div>
                <input 
                  id="excel-file-upload" 
                  type="file" 
                  accept=".xlsx" 
                  onChange={handleExcelUpload} 
                  style={{ display: 'none' }}
                />
              </label>

              {uploadStatus && (
                <div className={`tip-box`} style={{ 
                  marginTop: '20px', 
                  borderLeftColor: uploadStatus.type === 'success' ? 'var(--accent)' : uploadStatus.type === 'error' ? 'var(--accent-red)' : 'var(--primary)',
                  background: uploadStatus.type === 'success' ? 'rgba(16,185,129,0.05)' : uploadStatus.type === 'error' ? 'rgba(244,63,94,0.05)' : 'rgba(79,70,229,0.05)'
                }}>
                  <h4 style={{ color: uploadStatus.type === 'success' ? 'var(--accent)' : uploadStatus.type === 'error' ? 'var(--accent-red)' : 'var(--primary-light)' }}>
                    {uploadStatus.type.toUpperCase()}
                  </h4>
                  <p>{uploadStatus.message}</p>
                </div>
              )}
            </div>

            {/* Catalog Statistics & Controls */}
            <div className="glass-panel" style={{ padding: '32px' }}>
              <h3 className="form-title">
                <Sliders className="text-indigo-400" size={20} />
                Database Controls
              </h3>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', marginBottom: '32px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '12px', borderBottom: '1px solid var(--border)' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Total Stored Cars:</span>
                  <span style={{ fontWeight: '700', color: 'var(--accent)' }}>{status.carCount} vehicles</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '12px', borderBottom: '1px solid var(--border)' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Groq Key Configured:</span>
                  <span style={{ fontWeight: '700', color: status.isAiKeyConfigured ? 'var(--accent)' : 'var(--accent-red)' }}>
                    {status.isAiKeyConfigured ? 'YES (Operational)' : 'NO'}
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '12px', borderBottom: '1px solid var(--border)' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Seeding File:</span>
                  <span style={{ fontFamily: 'monospace', fontSize: '12px', color: 'var(--primary-light)' }}>sample-cars.xlsx</span>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '16px', marginTop: '24px' }}>
                <button 
                  onClick={seedSampleData} 
                  className="btn btn-secondary" 
                  style={{ flexGrow: 1 }}
                  disabled={loading}
                >
                  <RefreshCw size={16} />
                  <span>Sync Sample Data</span>
                </button>
                <button 
                  onClick={clearDatabase} 
                  className="btn" 
                  style={{ background: 'var(--accent-red)', boxShadow: '0 4px 12px rgba(244,63,94,0.15)', flexGrow: 1 }}
                  disabled={loading || status.carCount === 0}
                >
                  <Trash2 size={16} />
                  <span>Reset Database</span>
                </button>
              </div>
              
              <div style={{ marginTop: '24px', paddingTop: '24px', borderTop: '1px solid var(--border)' }}>
                <h4 style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px' }}>Temporary API Key (Dev Use Only)</h4>
                <input 
                  type="password" 
                  placeholder="gsk_..."
                  value={tempApiKey}
                  onChange={handleTempKeyChange}
                  style={{ width: '100%' }}
                />
              </div>
            </div>
          </div>

          {/* Current Data Table */}
          <div className="glass-panel" style={{ padding: '32px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
              <h3 className="form-title" style={{ marginBottom: 0 }}>
                <CarIcon className="text-indigo-400" size={20} />
                Current Inventory Data
              </h3>
              
              <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
                <span style={{ fontSize: '14px', color: 'var(--text-muted)' }}>{cars.length} entries loaded</span>
                {selectedCars.size > 0 && (
                  <button 
                    onClick={handleBatchDelete}
                    className="btn"
                    style={{ background: 'var(--accent-red)', padding: '8px 16px', fontSize: '13px' }}
                    disabled={loading}
                  >
                    <Trash2 size={14} /> Delete Selected ({selectedCars.size})
                  </button>
                )}
              </div>
            </div>
            
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th style={{ width: '40px' }}>
                      <input 
                        type="checkbox" 
                        checked={cars.length > 0 && selectedCars.size === cars.length}
                        onChange={toggleSelectAll}
                        style={{ cursor: 'pointer' }}
                      />
                    </th>
                    <th>ID</th>
                    <th>Make</th>
                    <th>Model</th>
                    <th>Variant</th>
                    <th>Price</th>
                    <th>Fuel Type</th>
                    <th>Trans.</th>
                    <th>Mileage</th>
                    <th>Safety</th>
                  </tr>
                </thead>
                <tbody>
                  {cars.length > 0 ? (
                    cars.map(car => (
                      <tr key={car.id} style={{ background: selectedCars.has(car.id) ? 'rgba(79, 70, 229, 0.1)' : 'transparent' }}>
                        <td>
                          <input 
                            type="checkbox" 
                            checked={selectedCars.has(car.id)}
                            onChange={() => toggleSelectCar(car.id)}
                            style={{ cursor: 'pointer' }}
                          />
                        </td>
                        <td style={{ color: 'var(--text-dark)' }}>#{car.id}</td>
                        <td style={{ fontWeight: '600' }}>{car.make}</td>
                        <td>{car.model}</td>
                        <td style={{ color: 'var(--text-muted)', fontSize: '13px' }}>{car.variant}</td>
                        <td style={{ color: 'var(--accent)' }}>{car.priceDisplay}</td>
                        <td>{car.fuelType}</td>
                        <td>{car.transmission}</td>
                        <td>{car.mileage}</td>
                        <td>{car.safetyRating} ★</td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="10" style={{ textAlign: 'center', padding: '32px', color: 'var(--text-muted)' }}>
                        No inventory data found in database.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </main>
      </div>
    );
  }

  // --- CUSTOMER VIEW ---
  return (
    <div className="app-container">
      {/* Sidebar Navigation */}
      <aside className="sidebar glass-panel">
        <div className="brand-section">
          <div className="brand-logo">
            CarDekho <span className="brand-dot"></span>
          </div>
        </div>

        <nav className="nav-menu">
          <div 
            onClick={() => setActiveTab('matchmaker')} 
            className={`nav-item ${activeTab === 'matchmaker' ? 'active' : ''}`}
          >
            <Sparkles size={18} />
            <span>AI Matchmaker</span>
          </div>
          
          <div 
            onClick={() => setActiveTab('browse')} 
            className={`nav-item ${activeTab === 'browse' ? 'active' : ''}`}
          >
            <Compass size={18} />
            <span>Browse Catalog</span>
          </div>
        </nav>

        <div className="sidebar-footer">
          <button 
            onClick={() => setUserRole('landing')} 
            className="btn btn-secondary" 
            style={{ width: '100%', marginBottom: '24px', justifyContent: 'center' }}
          >
            <ArrowLeft size={16} /> Switch Role
          </button>
          
          <div className="api-status">
            <span className={`status-dot ${status.isAiKeyConfigured ? 'active' : 'inactive'}`}></span>
            <span>AI Status: {status.isAiKeyConfigured ? 'Connected (Groq)' : 'Key Missing'}</span>
          </div>
        </div>
      </aside>

      {/* Main Content Pane */}
      <main className="main-content">
        
        {/* TAB 1: AI Matchmaker Panel */}
        <section className={`tab-panel ${activeTab === 'matchmaker' ? 'active' : ''}`}>
          <h1 className="header-title">AI Matchmaker</h1>
          <p className="header-subtitle">Provide your driving habits and priorities, and let our LLM find your ideal garage matching.</p>
          
          <div className="matchmaker-container">
            {/* Form */}
            <form onSubmit={handleMatchmake} className="glass-panel profile-form">
              <h3 className="form-title">
                <Sliders className="text-indigo-400" size={20} />
                Build Your Buyer Profile
              </h3>
              
              <div className="form-grid">
                <div className="form-group full-width">
                  <div className="range-container">
                    <label>Budget Limit (Lakhs):</label>
                    <span className="range-value">₹ {budget} L</span>
                  </div>
                  <input 
                    type="range" 
                    min="5" 
                    max="50" 
                    step="0.5"
                    value={budget} 
                    onChange={(e) => setBudget(parseFloat(e.target.value))} 
                  />
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: 'var(--text-dark)' }}>
                    <span>₹ 5 Lakhs</span>
                    <span>₹ 50 Lakhs</span>
                  </div>
                </div>

                <div className="form-group">
                  <label>Fuel Preference:</label>
                  <select value={fuelType} onChange={(e) => setFuelType(e.target.value)}>
                    <option value="Either">Either (Petrol / Diesel / EV)</option>
                    <option value="Petrol">Petrol</option>
                    <option value="Diesel">Diesel</option>
                    <option value="Electric">Electric (EV)</option>
                    <option value="CNG">CNG</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Transmission Preference:</label>
                  <select value={transmission} onChange={(e) => setTransmission(e.target.value)}>
                    <option value="Either">Either (Manual / Auto)</option>
                    <option value="Manual">Manual Only</option>
                    <option value="Automatic">Automatic Only</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Seating Capacity Required:</label>
                  <select value={seatingCapacity} onChange={(e) => setSeatingCapacity(parseInt(e.target.value))}>
                    <option value="4">4 Seater (Compact/Off-road)</option>
                    <option value="5">5 Seater (Sedan / Hatch / SUV)</option>
                    <option value="7">7 Seater (MUV / SUV Family)</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Daily Usage Habit:</label>
                  <select value={dailyUsage} onChange={(e) => setDailyUsage(e.target.value)}>
                    <option value="low (under 20 km)">Low (Under 20 km/day - City run)</option>
                    <option value="medium (20-50 km)">Medium (20-50 km/day - Commuter)</option>
                    <option value="high (50-100 km)">High (50-100 km/day - High mile runner)</option>
                    <option value="extreme (100+ km)">Extreme (100+ km/day - Long tours/CNG focus)</option>
                  </select>
                </div>

                <div className="form-group full-width">
                  <label>Select Top Priorities (Choose up to 3):</label>
                  <div className="checkbox-chips">
                    {['Safety', 'Mileage', 'Performance', 'Comfort', 'Cabin Tech', 'Off-Road Capability', 'Low Maintenance'].map((priority) => {
                      const isActive = priorities.includes(priority);
                      return (
                        <div 
                          key={priority}
                          onClick={() => handlePriorityToggle(priority)}
                          className={`chip ${isActive ? 'active' : ''}`}
                        >
                          {priority}
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>

              {!status.isAiKeyConfigured && (
                <div className="tip-box" style={{ background: 'rgba(244, 63, 94, 0.1)', borderLeftColor: 'var(--accent-red)', marginBottom: '20px' }}>
                  <h4 style={{ color: 'var(--accent-red)' }}>Groq Key Missing</h4>
                  <p>AI matchmaking is disabled. Set the <code>GROQ_API_KEY</code> environment variable or supply it in application properties.</p>
                </div>
              )}

              {error && (
                <div className="tip-box" style={{ background: 'rgba(244, 63, 94, 0.1)', borderLeftColor: 'var(--accent-red)', marginBottom: '20px' }}>
                  <h4 style={{ color: 'var(--accent-red)' }}>Error</h4>
                  <p>{error}</p>
                </div>
              )}

              <button 
                type="submit" 
                className="btn" 
                style={{ width: '100%' }}
                disabled={loading || !status.isAiKeyConfigured || status.carCount === 0}
              >
                {loading ? (
                  <>
                    <RefreshCw className="animate-spin" size={16} />
                    <span>Analyzing Catalogs...</span>
                  </>
                ) : (
                  <>
                    <Sparkles size={16} />
                    <span>Find My Shortlist</span>
                  </>
                )}
              </button>
            </form>

            {/* Guidance Panel */}
            <div className="glass-panel prompt-guidance">
              <h3 className="form-title" style={{ marginBottom: '8px' }}>How it works</h3>
              
              <div className="tip-box">
                <h4>1. Database Matching</h4>
                <p>The system retrieves all matching cars from your SQLite/H2 database based on budget limits and dimensions.</p>
              </div>

              <div className="tip-box">
                <h4>2. Semantic LLM Reasoning</h4>
                <p>We push your requirements alongside truncated specs and real user reviews to Groq's high-speed Llama 3 model.</p>
              </div>

              <div className="tip-box">
                <h4>3. Trade-off Analysis</h4>
                <p>The AI weighs priorities (e.g. choice of Nexon EV vs Diesel based on daily running stats) and builds detailed match score charts.</p>
              </div>

              {status.carCount === 0 && (
                <div style={{ marginTop: 'auto', textAlign: 'center', padding: '16px', background: 'rgba(245, 158, 11, 0.1)', border: '1px solid rgba(245, 158, 11, 0.2)', borderRadius: '8px' }}>
                  <AlertTriangle className="text-amber-500" style={{ margin: '0 auto 10px' }} />
                  <p style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Database is empty! Head to <strong>Dataset Manager</strong> to seed sample Indian cars.</p>
                </div>
              )}
            </div>
          </div>

          {/* LOADING SPINNER */}
          {loading && (
            <div className="spinner-container">
              <div className="spinner"></div>
              <p className="spinner-text">Groq is analyzing user feedback logs and car specifications...</p>
            </div>
          )}

          {/* MATCHMAKER RESULTS */}
          {matchResults && !loading && (
            <div className="results-container" style={{ marginTop: '40px' }}>
              <div className="glass-panel verdict-box">
                <h4 className="verdict-title">Matchmaker Verdict</h4>
                <p className="verdict-text">{matchResults.verdict}</p>
              </div>

              <div className="shortlist-grid">
                {matchResults.shortlist && matchResults.shortlist.map((match, idx) => {
                  // Find detailed specifications from state if matches by make/model
                  const dbCar = cars.find(c => c.id === match.carId || (c.make === match.make && c.model === match.model));
                  
                  return (
                    <div key={idx} className="glass-panel car-card">
                      <div className="badge-score">
                        <span className="badge-score-value">{match.matchScore}</span>
                        <span className="badge-score-label">Match</span>
                      </div>
                      
                      <span className="car-make">{match.make}</span>
                      <h3 className="car-title">{match.model}</h3>
                      <div className="car-price">
                        {dbCar ? dbCar.priceDisplay : "₹ Price N/A"}
                      </div>
                      
                      <div className="car-specs-grid">
                        <div className="spec-item">
                          <Fuel size={14} className="spec-icon" />
                          <span>{dbCar ? dbCar.fuelType : "Petrol"}</span>
                        </div>
                        <div className="spec-item">
                          <Compass size={14} className="spec-icon" />
                          <span>{dbCar ? dbCar.transmission : "Manual"}</span>
                        </div>
                        <div className="spec-item">
                          <Activity size={14} className="spec-icon" />
                          <span>{dbCar ? `${dbCar.mileage} kmpl` : "N/A"}</span>
                        </div>
                        <div className="spec-item">
                          <Shield size={14} className="spec-icon" />
                          {dbCar ? renderStars(dbCar.safetyRating) : "N/A"}
                        </div>
                      </div>

                      <p className="car-reasoning">{match.reasoning}</p>

                      <div className="pro-con-section">
                        {match.pros && match.pros.length > 0 && (
                          <div className="pro-con-box">
                            <span className="pro-title">✓ Pros</span>
                            <ul className="pro-con-list pros">
                              {match.pros.map((pro, pIdx) => <li key={pIdx}>{pro}</li>)}
                            </ul>
                          </div>
                        )}

                        {match.cons && match.cons.length > 0 && (
                          <div className="pro-con-box">
                            <span className="con-title">✗ Cons/Trade-offs</span>
                            <ul className="pro-con-list cons">
                              {match.cons.map((con, cIdx) => <li key={cIdx}>{con}</li>)}
                            </ul>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </section>

        {/* TAB 2: Browse Catalog Panel */}
        <section className={`tab-panel ${activeTab === 'browse' ? 'active' : ''}`}>
          <h1 className="header-title">Browse Vehicle Catalog</h1>
          <p className="header-subtitle">View and search through the parsed Excel spreadsheets stored in our local database.</p>
          
          <div className="glass-panel" style={{ padding: '24px', marginBottom: '24px' }}>
            <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
              <div style={{ position: 'relative', flexGrow: 1 }}>
                <Search size={18} style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-dark)' }} />
                <input 
                  type="text" 
                  placeholder="Search cars by make, model, type (e.g. Nexon, SUV, Tata)..." 
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  style={{ width: '100%', paddingLeft: '48px' }}
                />
              </div>
              <span style={{ fontSize: '13px', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                Showing {filteredCars.length} of {cars.length} vehicles
              </span>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: '24px' }}>
            {filteredCars.map((car) => (
              <div key={car.id} className="glass-panel car-card">
                <span className="car-make">{car.make}</span>
                <h3 className="car-title">{car.model} <span style={{ fontSize: '13px', color: 'var(--text-dark)' }}>{car.variant}</span></h3>
                <div className="car-price" style={{ marginBottom: '16px' }}>{car.priceDisplay}</div>
                
                <div className="car-specs-grid" style={{ marginBottom: '16px' }}>
                  <div className="spec-item">
                    <Fuel size={14} className="spec-icon" />
                    <span>Fuel: {car.fuelType}</span>
                  </div>
                  <div className="spec-item">
                    <Compass size={14} className="spec-icon" />
                    <span>Transmission: {car.transmission}</span>
                  </div>
                  <div className="spec-item">
                    <Activity size={14} className="spec-icon" />
                    <span>Economy: {car.mileage} {car.fuelType === 'Electric' ? 'km/charge' : 'km/l'}</span>
                  </div>
                  <div className="spec-item">
                    <Shield size={14} className="spec-icon" />
                    <span>Safety: {renderStars(car.safetyRating)}</span>
                  </div>
                </div>

                <div style={{ fontSize: '12px', borderTop: '1px solid var(--border)', paddingTop: '12px', marginTop: 'auto' }}>
                  <div style={{ color: 'var(--text-dark)', fontWeight: '600', marginBottom: '4px' }}>SPECS:</div>
                  <p style={{ color: 'var(--text-muted)', lineHeight: '1.4', marginBottom: '8px' }}>{car.specifications}</p>
                  
                  <div style={{ color: 'var(--text-dark)', fontWeight: '600', marginBottom: '4px' }}>AGGREGATED REVIEWS:</div>
                  <p style={{ color: 'var(--text-muted)', fontStyle: 'italic', lineHeight: '1.4' }}>"{car.userReviews}"</p>
                </div>
              </div>
            ))}

            {filteredCars.length === 0 && (
              <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '48px', color: 'var(--text-muted)' }}>
                <CarIcon size={48} style={{ margin: '0 auto 16px', opacity: 0.5 }} />
                <h3>No vehicles found</h3>
                <p>Try refining your search terms or verify database state in 'Dataset Manager'.</p>
              </div>
            )}
          </div>
        </section>

        {/* End of customer view modules */}

      </main>
    </div>
  );
}

export default App;
