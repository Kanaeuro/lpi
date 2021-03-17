import java.text.Normalizer;
import java.util.*;

class Constant {
    String name;
    public Constant(String name) {
        this.name = name;
    }
    public String name() {
        return this.name;
    }
    public String eval(Structure m) {
        return m.iC(name());
    }
    @Override
    public String toString() {
        return name();
    }
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (getClass() != other.getClass()) return false;
        Constant otherC = (Constant) other;
        return name().equals(otherC.name());
    }
}

class Formula {
    public List<Formula> subfs() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "Formula";
    }

    public boolean isTrue(Structure m) {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return true;
    }

    public int deg() {
        return 0;
    }

    public Set<AtomicFormula> atoms() {
        return new HashSet<AtomicFormula>();
    }

    public Set<String> constants() {
        return new HashSet<String>();
    }

    public Set<String> predicates() {
        return new HashSet<String>();
    }

    public final Cnf toCnf() {
        return new Cnf();
    }
    public Cnf nnfToCnf() {
        return new Cnf();
    }
    public Formula toNnf() {
        return new Formula();
    }
}

class AtomicFormula extends Formula {
    AtomicFormula() {}
}

class PredicateAtom extends AtomicFormula {
    private final String name;
    private final List<Constant> args;

    PredicateAtom(String name, List<Constant> args) {
        this.name = name;
        this.args = args;
    }

    String name() {
        return name;
    }

    List<Constant> arguments() {
        return args;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(name);
        result.append("(");
        for (int i = 0; i < args.size(); i++) {
            if (i != 0) result.append(",");
            result.append(args.get(i));
        }
        result.append(")");
        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PredicateAtom that = (PredicateAtom) o;
        return Objects.equals(name, that.name) && Objects.equals(args, that.args);
    }

    @Override
    public Set<String> constants() {
        HashSet<String> result = new HashSet<String>();
        args.forEach(x -> result.add(x.name()));
        return result;
    }

    @Override
    public Set<String> predicates() {
        return new HashSet<String>(Collections.singletonList(name));
    }

    @Override
    public Set<AtomicFormula> atoms() {
        return new HashSet<AtomicFormula>(Collections.singletonList(this));
    }

    @Override
    public boolean isTrue(Structure m) {
        List<String> constList = new ArrayList<String>();
        args.forEach(x -> constList.add(m.iC(x.name())));
        return m.iP(name).contains(constList);
    }
    public Formula toNnf() {
        return this;
    }
    public Cnf nnfToCnf() {
        Literal l = new Literal(this);
        List<Clause> cl = new ArrayList<>();
        cl.add(new Clause(l));
        return new Cnf(cl);
    }
}

class EqualityAtom extends AtomicFormula {
    private Constant left, right;

    EqualityAtom(Constant left, Constant right) {
        this.left = left;
        this.right = right;
    }

    Constant left() {
        return left;
    }

    Constant right() {
        return right;
    }

    @Override
    public String toString() {
        return left().toString() + "=" + right().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EqualityAtom that = (EqualityAtom) o;
        return Objects.equals(left, that.left) && Objects.equals(right, that.right);
    }

    @Override
    public Set<String> constants() {
        return new HashSet<>(Arrays.asList(left.name(), right.name()));
    }

    @Override
    public Set<AtomicFormula> atoms() {
        return new HashSet<>(Collections.singletonList(this));
    }

    @Override
    public boolean isTrue(Structure m) {
        return m.iC(left.name()).equals(m.iC(right.name()));
    }
}

class Negation extends Formula {
    private final Formula originalFormula;
    Negation(Formula originalFormula) {
        this.originalFormula = originalFormula;
    }

    public Formula originalFormula() {
        return this.originalFormula;
    }

    @Override
    public String toString() {
        return "-" + originalFormula.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Negation negation = (Negation) o;
        return Objects.equals(originalFormula, negation.originalFormula);
    }

    @Override
    public List<Formula> subfs() {
        return new ArrayList<>(Collections.singletonList(originalFormula));
    }

    @Override
    public Set<String> constants() {
        return originalFormula.constants();
    }

    @Override
    public Set<String> predicates() {
        return originalFormula.predicates();
    }

    @Override
    public Set<AtomicFormula> atoms() {
        return originalFormula.atoms();
    }

    @Override
    public boolean isTrue(Structure m) {
        return !originalFormula.isTrue(m);
    }

    @Override
    public int deg() {
        return originalFormula.deg() + 1;
    }
    public Formula toNnf() {
        if ( originalFormula instanceof AtomicFormula){
            return originalFormula;
        }
        else if (originalFormula instanceof Negation){
            return originalFormula.toNnf();
        }
        else if (originalFormula instanceof Conjunction){
            List<Formula> dis = new ArrayList<>();
            for (Formula f: originalFormula.subfs()){
                dis.add(new Negation(f));
            }
            return new Disjunction(dis).toNnf();
        }
        else if (originalFormula instanceof Disjunction){
            List<Formula> con = new ArrayList<>();
            for (Formula f: originalFormula.subfs()){
                con.add(new Negation(f));
            }
            return new Conjunction(con).toNnf();
        }
        else if (originalFormula instanceof Implication){
            List<Formula> impl = new ArrayList<>();
            impl.add(new Negation(((Implication) originalFormula).leftSide()));
            impl.add(((Implication) originalFormula).rightSide());
            return new Negation(new Disjunction(impl)).toNnf();
        }
        else{
            List<Formula> ls = new ArrayList<>();
            List<Formula> rs = new ArrayList<>();
            ls.add(((Equivalence) originalFormula).leftSide());
            ls.add(new Negation(((Equivalence) originalFormula).rightSide()));
            Formula l = new Conjunction(ls);
            rs.add(((Equivalence) originalFormula).rightSide());
            rs.add(new Negation(((Equivalence) originalFormula).leftSide()));
            Formula r = new Conjunction(rs);
            List<Formula> res = new ArrayList<>();
            res.add(l);
            res.add(r);
            return new Disjunction(res).toNnf();
        }
    }
    public Cnf nnfToCnf() {
        Literal l = new Literal((AtomicFormula) originalFormula, true);
        List<Clause> cl = new ArrayList<>();
        cl.add(new Clause(l));
        return new Cnf(cl);
    }
}

class Disjunction extends Formula {
    private List<Formula> disjuncts;

    Disjunction(List<Formula> disjuncts) {
        this.disjuncts = disjuncts;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("(");
        for (int i = 0; i < disjuncts.size(); i++) {
            if (i != 0) result.append("|");
            result.append(disjuncts.get(i).toString());
        }
        result.append(")");
        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Disjunction that = (Disjunction) o;
        return Objects.equals(disjuncts, that.disjuncts);
    }

    @Override
    public List<Formula> subfs() {
        return disjuncts;
    }

    @Override
    public Set<String> constants() {
        Set<String> result = new HashSet<>();
        for (Formula f : disjuncts) {
            result.addAll(f.constants());
        }
        return result;
    }

    @Override
    public Set<String> predicates() {
        Set<String> result = new HashSet<>();
        for (Formula f : disjuncts) {
            result.addAll(f.predicates());
        }
        return result;
    }

    @Override
    public Set<AtomicFormula> atoms() {
        Set<AtomicFormula> result = new HashSet<AtomicFormula>();
        for (Formula f : disjuncts) {
            result.addAll(f.atoms());
        }
        return result;
    }

    @Override
    public boolean isTrue(Structure m) {
        for (Formula f : disjuncts) {
            if (f.isTrue(m)) return true;
        }
        return false;
    }

    @Override
    public int deg() {
        int result = 0;
        for (Formula f : disjuncts) {
            result += f.deg();
        }
        return result + 1;
    }
    public Formula toNnf() {
        List<Formula> formulas= new ArrayList<Formula>();
        for (Formula f: disjuncts){
            formulas.add(f.toNnf());
        }
        return new Disjunction(formulas);
    }
    public Cnf nnfToCnf() {
        Cnf res = new Cnf();
        for (Formula f: disjuncts){
            res.addAll(f.toNnf().toCnf());
        }
        return res;
    }
}

class Conjunction extends Formula {
    private List<Formula> conjucts;

    Conjunction(List<Formula> conjuncts) {
        this.conjucts = conjuncts;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("(");
        for (int i = 0; i < conjucts.size(); i++) {
            if (i != 0) result.append("&");
            result.append(conjucts.get(i).toString());
        }
        result.append(")");
        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Conjunction that = (Conjunction) o;
        return Objects.equals(conjucts, that.conjucts);
    }

    @Override
    public List<Formula> subfs() {
        return conjucts;
    }

    @Override
    public Set<String> constants() {
        Set<String> result = new HashSet<String>();
        for (Formula f : conjucts) {
            result.addAll(f.constants());
        }
        return result;
    }

    @Override
    public Set<String> predicates() {
        Set<String> result = new HashSet<String>();
        for (Formula f : conjucts) {
            result.addAll(f.predicates());
        }
        return result;
    }

    @Override
    public Set<AtomicFormula> atoms() {
        Set<AtomicFormula> result = new HashSet<AtomicFormula>();
        for (Formula f : conjucts) {
            result.addAll(f.atoms());
        }
        return result;
    }

    @Override
    public boolean isTrue(Structure m) {
        for (Formula f : conjucts) {
            if (!f.isTrue(m)) return false;
        }
        return true;
    }

    @Override
    public int deg() {
        int result = 0;
        for (Formula f : conjucts) {
            result += f.deg();
        }
        return result + 1;
    }
    public Formula toNnf() {
        List<Formula> formulas= new ArrayList<Formula>();
        for (Formula f: conjucts){
            formulas.add(f.toNnf());
        }
        return new Conjunction(formulas);
    }
    public Cnf nnfToCnf() {
        Cnf res = new Cnf();
        for (Formula f: conjucts){
            res.addAll(f.toNnf().toCnf());
        }
        return res;
    }
}

class BinaryFormula extends Formula {
    private Formula left, right;

    BinaryFormula(Formula left, Formula right) {
        this.left = left;
        this.right = right;
    }

    public Formula leftSide() {
        return this.left;
    }

    public Formula rightSide() {
        return this.right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BinaryFormula that = (BinaryFormula) o;
        return Objects.equals(left, that.left) && Objects.equals(right, that.right);
    }

    @Override
    public List<Formula> subfs() {
        return new ArrayList<Formula>(Arrays.asList(leftSide(), rightSide()));
    }

    @Override
    public Set<String> constants() {
        Set<String> result = new HashSet<String>();
        result.addAll(leftSide().constants());
        result.addAll(rightSide().constants());
        return result;
    }

    @Override
    public Set<String> predicates() {
        Set<String> result = new HashSet<String>();
        result.addAll(leftSide().predicates());
        result.addAll(rightSide().predicates());
        return result;
    }

    @Override
    public Set<AtomicFormula> atoms() {
        Set<AtomicFormula> result = new HashSet<AtomicFormula>();
        result.addAll(leftSide().atoms());
        result.addAll(rightSide().atoms());
        return result;
    }

    @Override
    public int deg() {
        return leftSide().deg() + rightSide().deg() + 1;
    }
}

class Implication extends BinaryFormula {
    Implication(Formula left, Formula right) {
        super(left, right);
    }

    @Override
    public String toString() {
        return "(" + leftSide().toString() + "->" + rightSide().toString() + ")";
    }

    @Override
    public boolean isTrue(Structure m) {
        return !leftSide().isTrue(m) || rightSide().isTrue(m);
    }

    public Formula toNnf() {
        List<Formula> formulas = new ArrayList<Formula>();
        formulas.add(new Negation(leftSide()));
        formulas.add(rightSide());
        return new Disjunction(formulas).toNnf();
    }
}

class Equivalence extends BinaryFormula {
    Equivalence(Formula left, Formula right) {
        super(left, right);
    }

    @Override
    public String toString() {
        return "(" + leftSide().toString() + "<->" + rightSide().toString() + ")";
    }

    @Override
    public boolean isTrue(Structure m) {
        return leftSide().isTrue(m) == rightSide().isTrue(m);
    }
    public Formula toNnf() {
        List<Formula> ls = new ArrayList<Formula>();
        List<Formula> rs =new ArrayList<Formula>();
        ls.add(leftSide());
        ls.add(rightSide());
        Formula l = new Conjunction(ls);
        rs.add(new Negation(leftSide()));
        rs.add(new Negation(rightSide()));
        Formula r = new Conjunction(rs);
        List<Formula> eqviv = new ArrayList<Formula>();
        return new Disjunction(eqviv);
    }
}