#include "baseline/tracker.hpp"
#define ase optimized_ase
#include "current/tracker.hpp"
#undef ase
#include <iostream>
#include <random>
#include <chrono>
#include <cstring>
#include <iomanip>
#include <cstdlib>
using Frame=std::vector<uint8_t>;
constexpr int W=180,H=180;
struct Trial{Frame seed;int x,y,r;std::vector<Frame> frames;};
Frame shifted(const Frame& a,int dx,int dy){Frame b(W*H,127);for(int y=0;y<H;y++)for(int x=0;x<W;x++){int xx=x-dx,yy=y-dy;if(xx>=0&&xx<W&&yy>=0&&yy<H)b[y*W+x]=a[yy*W+xx];}return b;}
std::vector<Trial> scenarios(){std::vector<Trial> all;std::mt19937 rng(8910);for(int z=0;z<16;z++){Trial t;t.seed.resize(W*H);for(auto& v:t.seed)v=rng()%256;t.x=z%4==0?25:90;t.y=z%4==1?155:90;t.r=8+z*4;for(int j=0;j<18;j++){auto b=shifted(t.seed,(j%7)-3,(j%9)-4);if(j==5)b=Frame(W*H,127);if(j==6)b=shifted(t.seed,22,-18);if(j==10){for(int k=0;k<11;k++)for(int l=0;l<11;l++)b[(t.y+k-5)*W+t.x+l-5]=127;}if(j==14)for(auto& v:b)v=uint8_t(20+v*.7);t.frames.push_back(std::move(b));}all.push_back(std::move(t));}return all;}
template<class T> double bench(const std::vector<Trial>& cases,int repeat){auto start=std::chrono::steady_clock::now();volatile float sink=0;for(int n=0;n<repeat;n++)for(auto& t:cases){T tr;tr.seed(t.seed.data(),W,H,t.x,t.y,t.r);for(auto& b:t.frames){auto q=tr.step(b.data(),W,H);if(q.valid)sink+=q.q;}}return std::chrono::duration<double,std::milli>(std::chrono::steady_clock::now()-start).count();}
int main(){auto cases=scenarios();int count=0;for(auto& t:cases){ase::Tracker a;optimized_ase::Tracker b;if(a.seed(t.seed.data(),W,H,t.x,t.y,t.r)!=b.seed(t.seed.data(),W,H,t.x,t.y,t.r))return 1;for(auto& f:t.frames){auto p=a.step(f.data(),W,H);auto q=b.step(f.data(),W,H);if(p.valid!=q.valid||p.x!=q.x||p.y!=q.y||p.q!=q.q){std::cerr<<"DIFFERENCE "<<count<<" "<<p.x<<" "<<q.x<<" "<<p.q<<" "<<q.q<<"\n";return 2;}++count;}}std::cout<<"EXACT_MATCH="<<count<<"/"<<count<<"\n";std::vector<double> old,newv;for(int j=0;j<5;j++){double a,b;if(j%2){b=bench<optimized_ase::Tracker>(cases,2);a=bench<ase::Tracker>(cases,2);}else{a=bench<ase::Tracker>(cases,2);b=bench<optimized_ase::Tracker>(cases,2);}old.push_back(a);newv.push_back(b);std::cout<<"trial="<<j+1<<" baseline_ms="<<a<<" optimized_ms="<<b<<"\n";}std::sort(old.begin(),old.end());std::sort(newv.begin(),newv.end());std::cout<<"MEDIAN baseline="<<old[2]<<" optimized="<<newv[2]<<" ratio="<<newv[2]/old[2]<<"\n";return newv[2]>old[2]*1.02?3:0;}
