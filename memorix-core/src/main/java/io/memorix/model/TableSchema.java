package io.memorix.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Defines database table schema for a memory plugin.
 * 
 * <p>Allows plugins to specify custom table structure, indexes, and constraints.
 * 
 * <p>Example:
 * <pre>{@code
 * TableSchema schema = TableSchema.builder()
 *     .tableName("documentation_memories")
 *     .vectorDimension(1536)
 *     .addCustomColumn("category VARCHAR(100)")
 *     .addCustomColumn("version VARCHAR(50)")
 *     .addCustomIndex("CREATE INDEX idx_docs_category ON documentation_memories(category)")
 *     .build();
 * }</pre>
 */
public class TableSchema {
    
    /**
     * Default schema - uses 'memories' table with standard structure.
     */
    public static final TableSchema DEFAULT = TableSchema.builder()
            .tableName("memories")
            .vectorDimension(1536)
            .build();
    
    private final String tableName;
    private final int vectorDimension;
    private final List<String> customColumns;
    private final List<String> customIndexes;
    
    private TableSchema(Builder builder) {
        this.tableName = builder.tableName;
        this.vectorDimension = builder.vectorDimension;
        this.customColumns = new ArrayList<>(builder.customColumns);
        this.customIndexes = new ArrayList<>(builder.customIndexes);
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public int getVectorDimension() {
        return vectorDimension;
    }
    
    public List<String> getCustomColumns() {
        return new ArrayList<>(customColumns);
    }
    
    public List<String> getCustomIndexes() {
        return new ArrayList<>(customIndexes);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String tableName = "memories";
        private int vectorDimension = 1536;
        private List<String> customColumns = new ArrayList<>();
        private List<String> customIndexes = new ArrayList<>();
        
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }
        
        public Builder vectorDimension(int vectorDimension) {
            this.vectorDimension = vectorDimension;
            return this;
        }
        
        public Builder customColumns(List<String> customColumns) {
            this.customColumns = new ArrayList<>(customColumns);
            return this;
        }
        
        public Builder addCustomColumn(String columnDefinition) {
            this.customColumns.add(columnDefinition);
            return this;
        }
        
        public Builder customIndexes(List<String> customIndexes) {
            this.customIndexes = new ArrayList<>(customIndexes);
            return this;
        }
        
        public Builder addCustomIndex(String indexDefinition) {
            this.customIndexes.add(indexDefinition);
            return this;
        }
        
        public TableSchema build() {
            Objects.requireNonNull(tableName, "tableName cannot be null");
            if (vectorDimension <= 0) {
                throw new IllegalArgumentException("vectorDimension must be positive");
            }
            return new TableSchema(this);
        }
    }
    
    @Override
    public String toString() {
        return "TableSchema{" +
                "tableName='" + tableName + '\'' +
                ", vectorDimension=" + vectorDimension +
                ", customColumns=" + customColumns.size() +
                ", customIndexes=" + customIndexes.size() +
                '}';
    }
}

